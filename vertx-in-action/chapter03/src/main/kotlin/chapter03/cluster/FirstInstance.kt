package chapter03.cluster

import chapter03.HeatSensor
import chapter03.HttpServer
import io.vertx.core.CompositeFuture
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

object FirstInstance {
  @JvmStatic
  fun main(args: Array<String>): Unit = runBlocking {
    try {
      val vertx = Vertx.clusteredVertx(vertxOptionsOf()).await()
      logger.info { "First instance has been started" }
      CompositeFuture.all(
        vertx.deployVerticle(HeatSensor::class.java.name, deploymentOptionsOf(instances = 4)),
        vertx.deployVerticle(HttpServer::class.java.name)
      ).await()
    } catch (e: Throwable) {
      logger.error(e) { "Could not start" }
    }
  }
}