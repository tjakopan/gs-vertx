package chapter02.deploy

import io.vertx.kotlin.coroutines.CoroutineVerticle
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class EmptyVerticle : CoroutineVerticle() {
  override suspend fun start() {
    logger.info { "Start" }
  }

  override suspend fun stop() {
    logger.info { "Stop" }
  }
}