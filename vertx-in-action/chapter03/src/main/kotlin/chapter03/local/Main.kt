package chapter03.local

import chapter03.HeatSensor
import chapter03.HttpServer
import chapter03.Listener
import chapter03.SensorData
import io.vertx.core.CompositeFuture
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking

object Main {
  @JvmStatic
  fun main(args: Array<String>): Unit = runBlocking {
    val vertx = Vertx.vertx()
    CompositeFuture.all(
      vertx.deployVerticle(HeatSensor::class.java.name, deploymentOptionsOf(instances = 4)),
      vertx.deployVerticle(Listener::class.java.name),
      vertx.deployVerticle(SensorData::class.java.name),
      vertx.deployVerticle(HttpServer::class.java.name)
    ).await()
  }
}