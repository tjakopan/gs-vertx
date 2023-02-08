package chapter05.callbacks

import chapter05.sensor.HeatSensor
import chapter05.snapshot.SnapshotService
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    vertx.deployVerticle(
      HeatSensor::class.java.name,
      deploymentOptionsOf(config = json { obj("http.port" to 3000) })
    )
    vertx.deployVerticle(
      HeatSensor::class.java.name,
      deploymentOptionsOf(config = json { obj("http.port" to 3001) })
    )
    vertx.deployVerticle(
      HeatSensor::class.java.name,
      deploymentOptionsOf(config = json { obj("http.port" to 3002) })
    )

    vertx.deployVerticle(SnapshotService::class.java.name)
    vertx.deployVerticle(CollectorService::class.java.name)
  }
}