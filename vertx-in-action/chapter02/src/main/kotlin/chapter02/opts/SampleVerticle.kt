package chapter02.opts

import io.vertx.core.Vertx
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class SampleVerticle : CoroutineVerticle() {
  override suspend fun start() {
    logger.info { "n = ${config.getInteger("n", -1)}" }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      val vertx = Vertx.vertx()
      for (i in 0..3) {
        val conf = json { obj("n" to i) }
        val opts = deploymentOptionsOf(config = conf, instances = i)
        vertx.deployVerticle(SampleVerticle::class.java.name, opts)
      }
    }
  }
}