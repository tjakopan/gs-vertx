package chapter03

import io.vertx.core.TimeoutStream
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import mu.KotlinLogging
import utilities.core.streams.handler
import utilities.core.http.endHandler
import utilities.core.http.requestHandler

private val logger = KotlinLogging.logger { }

class HttpServer : CoroutineVerticle() {
  override suspend fun start() {
    vertx.createHttpServer()
      .requestHandler(this) { req -> handler(req) }
      .listen(config.getInteger("port", 8080))
      .await()
  }

  private suspend fun handler(request: HttpServerRequest) {
    when {
      "/" == request.path() -> request.response().sendFile("index.html").await()
      "/sse" == request.path() -> sse(request)
      else -> request.response().statusCode = 404
    }
  }

  private suspend fun sse(request: HttpServerRequest) {
    val response = request.response().apply {
      putHeader("Content-Type", "text/event-stream")
      putHeader("Cache-Control", "no-cache")
      isChunked = true
    }

    val consumer = vertx.eventBus().consumer<JsonObject>("sensor.updates")
    consumer.handler { msg ->
      response.write("event: update\n")
      response.write("data: ${msg.body().encode()}\n\n")
    }

    val ticks: TimeoutStream = vertx.periodicStream(1000)
    ticks.handler(this) {
      try {
        val reply = vertx.eventBus().request<JsonObject>("sensor.average", "").await()
        response.write("event: average\n")
        response.write("data: ${reply.body().encode()}\n\n")
      } catch (e: Throwable) {
        logger.error(e) { "Error while requesting sensor average" }
      }
    }

    response.endHandler(this) {
      consumer.unregister().await()
      ticks.cancel()
    }
  }
}