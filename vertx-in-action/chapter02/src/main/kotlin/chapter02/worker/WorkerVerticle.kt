package chapter02.worker

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class WorkerVerticle : AbstractVerticle() {
  override fun start() {
    vertx.setPeriodic(10_000) {
      try {
        logger.info { "Zzz..." }
        Thread.sleep(8000)
        logger.info { "Up!" }
      } catch (e: InterruptedException) {
        logger.error(e) { "Woops" }
      }
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      val vertx = Vertx.vertx()
      val opts = deploymentOptionsOf(instances = 2, worker = true)
      vertx.deployVerticle(WorkerVerticle::class.java.name, opts).await()
    }
  }
}