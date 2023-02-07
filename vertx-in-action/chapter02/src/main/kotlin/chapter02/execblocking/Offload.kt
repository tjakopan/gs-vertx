package chapter02.execblocking

import io.vertx.core.AsyncResult
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class Offload : CoroutineVerticle() {
  override suspend fun start() {
    vertx.setPeriodic(5000) {
      logger.info { "Tick" }
      vertx.executeBlocking(::blockingCode, ::resultHandler)
    }
  }

  private fun blockingCode(promise: Promise<String>) {
    logger.info { "Blocking code running" }
    try {
      Thread.sleep(4000)
      logger.info { "Done!" }
      promise.complete("Ok!")
    } catch (e: InterruptedException) {
      promise.fail(e)
    }
  }

  private fun resultHandler(ar: AsyncResult<String>) {
    if (ar.succeeded()) logger.info { "Blocking code result: ${ar.result()}" }
    else logger.error(ar.cause()) { "Woops" }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      val vertx = Vertx.vertx()
      vertx.deployVerticle(Offload()).await()
    }
  }
}