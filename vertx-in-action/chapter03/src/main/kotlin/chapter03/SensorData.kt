package chapter03

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle

class SensorData : CoroutineVerticle() {
  private val lastValues: MutableMap<String, Double> = mutableMapOf()

  override suspend fun start() {
    val bus = vertx.eventBus()
    bus.consumer("sensor.updates", ::update)
    bus.consumer("sensor.average", ::average)
  }

  private fun update(message: Message<JsonObject>) {
    val json = message.body()
    lastValues[json.getString("id")] = json.getDouble("temp")
  }

  private fun average(message: Message<JsonObject>) {
    val avg = lastValues.values.average()
    val json = json { obj("average" to avg) }
    message.reply(json)
  }
}