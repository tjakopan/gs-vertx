package chapter01.snippets

import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object SynchronousEcho {
  @JvmStatic
  fun main(args: Array<String>) {
    val serverSocket = ServerSocket().apply {
      bind(InetSocketAddress(3000))
    }
    while (true) {
      val socket = serverSocket.accept()
      thread { clientHandler(socket) }
    }
  }

  private fun clientHandler(socket: Socket) =
    try {
      BufferedReader(InputStreamReader(socket.getInputStream())).use { bufferedReader ->
        PrintWriter(OutputStreamWriter(socket.getOutputStream())).use { printWriter ->
          var line = ""
          while ("/quit" != line) {
            line = bufferedReader.readLine()
            println("~ $line")
            printWriter.write("$line\n")
            printWriter.flush()
          }
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
}
