package tenksteps.activities

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class Main : CoroutineVerticle() {
  override suspend fun start() {
    try {
      vertx.deployVerticle(EventsVerticle()).await()
      vertx.deployVerticle(ActivityApiVerticle()).await()
      logger.info { "HTTP server started on port $HTTP_PORT" }
    } catch (e: Throwable) {
      logger.error(e) { "Woops" }
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      Vertx.vertx().deployVerticle(Main()).await()
    }
  }
}