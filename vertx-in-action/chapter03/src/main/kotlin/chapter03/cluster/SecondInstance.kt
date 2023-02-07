package chapter03.cluster

import chapter03.HeatSensor
import chapter03.HttpServer
import chapter03.Listener
import chapter03.SensorData
import io.vertx.core.CompositeFuture
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

object SecondInstance {
  @JvmStatic
  fun main(args: Array<String>): Unit = runBlocking {
    try {
      val vertx = Vertx.clusteredVertx(vertxOptionsOf()).await()
      logger.info { "Second instance has been started" }
      val conf = json { obj("port" to 8081) }
      CompositeFuture.all(
        vertx.deployVerticle(HeatSensor::class.java.name, deploymentOptionsOf(instances = 4)),
        vertx.deployVerticle(Listener::class.java.name),
        vertx.deployVerticle(SensorData::class.java.name),
        vertx.deployVerticle(HttpServer::class.java.name, deploymentOptionsOf(config = conf))
      ).await()
    } catch (e: Throwable) {
      logger.error(e) { "Could not start" }
    }
  }
}