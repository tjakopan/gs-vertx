package chapter05.snapshot

import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.kotlin.coroutines.CoroutineVerticle
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class SnapshotService : CoroutineVerticle() {
  override suspend fun start() {
    vertx.createHttpServer()
      .requestHandler { req ->
        if (badRequest(req)) req.response().setStatusCode(400).end()
        req.bodyHandler { buffer ->
          logger.info { "Latest temperatures: ${buffer.toJsonObject().encodePrettily()}" }
          req.response().end()
        }
      }
      .listen(config.getInteger("http.port", 4000))
  }

  private fun badRequest(req: HttpServerRequest): Boolean =
    req.method() != HttpMethod.POST || "application/json" != req.getHeader("Content-Type")
}