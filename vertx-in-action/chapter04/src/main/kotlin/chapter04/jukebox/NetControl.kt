package chapter04.jukebox

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import io.vertx.core.parsetools.RecordParser
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import mu.KotlinLogging
import utilities.core.connectHandler
import utilities.core.streams.handler

private val logger = KotlinLogging.logger { }

class NetControl : CoroutineVerticle() {
  override suspend fun start() {
    logger.info { "Start" }
    vertx.createNetServer()
      .connectHandler(this) { socket -> handleClient(socket) }
      .listen(3000)
      .await()
  }

  private suspend fun handleClient(socket: NetSocket) {
    logger.info { "New connection" }
    RecordParser.newDelimited("\n", socket)
      .handler(this) { buffer -> handleBuffer(socket, buffer) }
      .endHandler { logger.info { "Connection ended" } }
  }

  private suspend fun handleBuffer(socket: NetSocket, buffer: Buffer) {
    when (val command = buffer.toString()) {
      "/list" -> listCommand(socket)
      "/play" -> vertx.eventBus().send("jukebox.play", "")
      "/pause" -> vertx.eventBus().send("jukebox.pause", "")
      else -> if (command.startsWith("/schedule")) schedule(command) else socket.write("Unknown command\n")
    }
  }

  private fun schedule(command: String) {
    val track = command.substring(10)
    val json = json { obj("file" to track) }
    vertx.eventBus().send("jukebox.schedule", json)
  }

  private suspend fun listCommand(socket: NetSocket) {
    try {
      val reply = vertx.eventBus().request<JsonObject>("jukebox.list", "").await()
      val data = reply.body()
      data.getJsonArray("files").forEach { name -> socket.write("$name\n") }
    } catch (e: Throwable) {
      logger.error(e) { "/list error" }
    }
  }
}