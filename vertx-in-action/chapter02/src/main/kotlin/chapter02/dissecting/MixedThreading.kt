package chapter02.dissecting

import io.vertx.core.AbstractVerticle
import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger { }

class MixedThreading : AbstractVerticle() {

  override fun start() {
    val context = vertx.orCreateContext
    thread {
      try {

        run(context)
      } catch (e: InterruptedException) {
        logger.error(e) { "Woops" }
      }
    }
  }

  private fun run(context: Context) {
    val countDownLatch = CountDownLatch(1)
    logger.info { "I am in a non-Vert.x thread" }
    context.runOnContext {
      logger.info { "I am on the event-loop" }
      vertx.setTimer(1000) {
        logger.info { "This is the final countdown" }
        countDownLatch.countDown()
      }
    }
    logger.info { "Waiting on the countdown latch..." }
    countDownLatch.await()
    logger.info { "Bye!" }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      val vertx = Vertx.vertx()
      vertx.deployVerticle(MixedThreading()).await()
    }
  }
}