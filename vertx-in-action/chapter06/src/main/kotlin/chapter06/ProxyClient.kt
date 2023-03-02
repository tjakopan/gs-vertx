package chapter06

import io.vertx.core.Vertx
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import mu.KotlinLogging
import utilities.core.setPeriodic
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

class ProxyClient : CoroutineVerticle() {
  override suspend fun start() {
    val service = createSensorDataServiceProxy(vertx, "sensor.data-service")
    vertx.setPeriodic(this, 3.seconds) {
      val data = service.average().await()
      logger.info { "avg = ${data.getDouble("average")}" }
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val vertx = Vertx.vertx()
      vertx.deployVerticle(HeatSensor::class.java.name, deploymentOptionsOf(instances = 4))
      vertx.deployVerticle(DataVerticle())
      vertx.deployVerticle(ProxyClient())
    }
  }
}