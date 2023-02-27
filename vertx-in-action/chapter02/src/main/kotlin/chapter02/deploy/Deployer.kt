package chapter02.deploy

import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import mu.KotlinLogging
import utilities.core.setTimer
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger { }

class Deployer : CoroutineVerticle() {
  override suspend fun start() {
    var delay = 1000.milliseconds
    for (i in 0..49) {
      vertx.setTimer(this, delay) { deploy() }
      delay += 1000.milliseconds
    }
  }

  private suspend fun deploy() {
    try {
      val id = vertx.deployVerticle(EmptyVerticle()).await()
      logger.info { "Successfully deployed $id" }
      vertx.setTimer(this, 5000.milliseconds) { undeployLater(id) }
    } catch (e: Throwable) {
      logger.error(e) { "Error while deploying" }
    }
  }

  private suspend fun undeployLater(id: String) {
    try {
      vertx.undeploy(id).await()
      logger.info { "$id was un-deployed" }
    } catch (e: Throwable) {
      logger.error { "$id could not be un-deployed" }
    }
  }
}