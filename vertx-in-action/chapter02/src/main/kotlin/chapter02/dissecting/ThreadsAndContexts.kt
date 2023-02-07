package chapter02.dissecting

import io.vertx.core.Vertx
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

object ThreadsAndContexts {
  @JvmStatic
  fun main(args: Array<String>) {
    createAndRun()
    dataAndExceptions()
  }

  private fun createAndRun() {
    val vertx = Vertx.vertx()

    vertx.orCreateContext
      .runOnContext { logger.info { "ABC" } }

    vertx.orCreateContext
      .runOnContext { logger.info { "123" } }
  }

  private fun dataAndExceptions() {
    val vertx = Vertx.vertx()
    val ctx = vertx.orCreateContext
    ctx.put("foo", "bar")

    ctx.exceptionHandler { t ->
      if ("Tada" == t.message) logger.info { "Got a _Tada_ exception" }
      else logger.error(t) { "Woops" }
    }

    ctx.runOnContext { throw RuntimeException("Tada") }

    ctx.runOnContext { logger.info { "foo = ${ctx.get<String>("foo")}" } }
  }
}