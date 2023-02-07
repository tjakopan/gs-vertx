package chapter02.hello

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class HelloVerticle : CoroutineVerticle() {
  private var counter: Long = 1

  override suspend fun start() {
    vertx.setPeriodic(5000) { logger.info { "tick" } }

    vertx.createHttpServer()
      .requestHandler { req ->
        logger.info { "Request #${counter++} from ${req.remoteAddress().host()}" }
        req.response().end("Hello!")
      }
      .listen(8080)
      .await()

    logger.info { "Open http://localhost:8080/" }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      val vertx = Vertx.vertx()
      vertx.deployVerticle(HelloVerticle()).await()
    }
  }
}