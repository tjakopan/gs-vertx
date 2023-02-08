package chapter06

import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import java.util.*
import kotlin.random.Random
import kotlin.random.asJavaRandom

class HeatSensor : CoroutineVerticle() {
  private val sensorId: String = UUID.randomUUID().toString()
  private var temperature: Double = 21.0

  override suspend fun start() {
    scheduleNextUpdate()
  }

  private fun scheduleNextUpdate() {
    vertx.setTimer(Random.nextInt(5000) + 1000L) { update() }
  }

  private fun update() {
    temperature += (delta() / 10)
    val payload = json { obj("id" to sensorId, "temp" to temperature) }
    vertx.eventBus().publish("sensor.updates", payload)
    scheduleNextUpdate()
  }

  private fun delta(): Double =
    if (Random.nextInt() > 0) Random.asJavaRandom().nextGaussian()
    else -Random.asJavaRandom().nextGaussian()
}