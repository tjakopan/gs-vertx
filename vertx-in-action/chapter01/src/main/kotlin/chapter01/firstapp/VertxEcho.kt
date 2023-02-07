package chapter01.firstapp

import io.vertx.core.Vertx
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking

object VertxEcho {
  private var numberOfConnections: Int = 0

  @JvmStatic
  fun main(args: Array<String>): Unit = runBlocking {
    val vertx = Vertx.vertx()

    vertx.createNetServer()
      .connectHandler(::handleNewClient)
      .listen(3000)
      .await()

    vertx.setPeriodic(5000) { println(howMany()) }

    vertx.createHttpServer()
      .requestHandler { request -> request.response().end(howMany()) }
      .listen(8080)
      .await()
  }

  private fun handleNewClient(socket: NetSocket) {
    numberOfConnections++
    socket.handler { buffer ->
      socket.write(buffer)
      if (buffer.toString().endsWith("/quit\n")) socket.close()
    }
    socket.closeHandler { numberOfConnections-- }
  }

  private fun howMany(): String = "We now have $numberOfConnections connection"
}
