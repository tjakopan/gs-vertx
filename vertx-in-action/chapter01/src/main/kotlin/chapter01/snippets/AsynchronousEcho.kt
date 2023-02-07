package chapter01.snippets

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.Charset

object AsynchronousEcho {

  @JvmStatic
  fun main(args: Array<String>) {
    val selector = Selector.open()

    ServerSocketChannel.open().apply {
      bind(InetSocketAddress(3000))
      configureBlocking(false)
      register(selector, SelectionKey.OP_ACCEPT)
    }

    while (true) {
      selector.select()
      val it = selector.selectedKeys().iterator()
      while (it.hasNext()) {
        val key = it.next()
        if (key.isAcceptable) newConnection(selector, key)
        else if (key.isReadable) echo(key)
        else if (key.isWritable) continueEcho(selector, key)
        it.remove()
      }
    }
  }

  private class Context(
    val byteBuffer: ByteBuffer = ByteBuffer.allocate(512),
    var currentLine: String = "",
    var terminating: Boolean = false
  )

  private val contexts: MutableMap<SocketChannel, Context> = mutableMapOf()

  private fun newConnection(selector: Selector, key: SelectionKey) {
    val serverSocketChannel = key.channel() as ServerSocketChannel
    val socketChannel = serverSocketChannel.accept().apply {
      configureBlocking(false)
      register(selector, SelectionKey.OP_READ)
    }
    contexts[socketChannel] = Context()
  }

  private val QUIT: Regex = "(\\r)?(\\n)?/quit(\\r)?(\\n)?$".toRegex()

  private fun echo(key: SelectionKey) {
    val socketChannel = key.channel() as SocketChannel
    val context = contexts[socketChannel]
    try {
      socketChannel.read(context!!.byteBuffer)
      context.byteBuffer.flip()
      context.currentLine = context.currentLine + Charset.defaultCharset().decode(context.byteBuffer)
      if (QUIT.matches(context.currentLine)) {
        context.terminating = true
      } else if (context.currentLine.length > 16) {
        context.currentLine = context.currentLine.substring(8)
      }
      context.byteBuffer.flip()
      val count = socketChannel.write(context.byteBuffer)
      if (count < context.byteBuffer.limit()) {
        key.cancel()
        socketChannel.register(key.selector(), SelectionKey.OP_WRITE)
      } else {
        context.byteBuffer.clear()
        if (context.terminating) cleanup(socketChannel)
      }
    } catch (e: IOException) {
      e.printStackTrace()
      cleanup(socketChannel)
    }
  }

  private fun cleanup(socketChannel: SocketChannel) {
    socketChannel.close()
    contexts.remove(socketChannel)
  }

  private fun continueEcho(selector: Selector, key: SelectionKey) {
    val socketChannel = key.channel() as SocketChannel
    val context = contexts[socketChannel]
    try {
      val remainingBytes = context!!.byteBuffer.limit() - context.byteBuffer.position()
      val count = socketChannel.write(context.byteBuffer)
      if (count == remainingBytes) {
        context.byteBuffer.clear()
        key.cancel()
        if (context.terminating) cleanup(socketChannel)
        else socketChannel.register(selector, SelectionKey.OP_READ)
      }
    } catch (e: IOException) {
      e.printStackTrace()
      cleanup(socketChannel)
    }
  }
}
