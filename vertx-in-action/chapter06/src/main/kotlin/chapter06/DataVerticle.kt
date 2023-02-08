package chapter06

import io.vertx.core.AbstractVerticle
import io.vertx.serviceproxy.ServiceBinder

class DataVerticle : AbstractVerticle() {
  override fun start() {
    ServiceBinder(vertx)
      .setAddress("sensor.data-service")
      .register(SensorDataService::class.java, createSensorDataService(vertx))
  }
}