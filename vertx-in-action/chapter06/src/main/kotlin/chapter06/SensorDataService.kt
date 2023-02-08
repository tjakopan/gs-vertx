package chapter06

import io.vertx.codegen.annotations.ProxyGen
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

@ProxyGen
interface SensorDataService {
  fun valueFor(sensorId: String): Future<JsonObject>
  fun average(): Future<JsonObject>
}

fun createSensorDataService(vertx: Vertx): SensorDataService = SensorDataServiceImpl(vertx)

fun createSensorDataServiceProxy(vertx: Vertx, address: String): SensorDataService =
  SensorDataServiceVertxEBProxy(vertx, address)