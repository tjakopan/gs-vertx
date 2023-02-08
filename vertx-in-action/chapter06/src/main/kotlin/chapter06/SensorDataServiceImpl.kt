package chapter06

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class SensorDataServiceImpl(vertx: Vertx) : SensorDataService {
  private val lastValues: MutableMap<String, Double> = mutableMapOf()

  init {
    vertx.eventBus().consumer<JsonObject>("sensor.updates") { message ->
      val json = message.body()
      lastValues[json.getString("id")] = json.getDouble("temp")
    }
  }

  override fun valueFor(sensorId: String): Future<JsonObject> =
    if (lastValues.containsKey(sensorId))
      Future.succeededFuture(json { obj("sensorId" to sensorId, "value" to lastValues[sensorId]) })
    else Future.failedFuture("No value has been observed for $sensorId")

  override fun average(): Future<JsonObject> {
    var avg = lastValues.values.average()
    if (avg.isNaN()) avg = 0.0
    return Future.succeededFuture(json { obj("average" to avg) })
  }
}