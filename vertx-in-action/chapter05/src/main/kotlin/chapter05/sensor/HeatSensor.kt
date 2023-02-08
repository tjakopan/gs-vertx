package chapter05.sensor

import io.vertx.core.http.HttpServerRequest
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import java.util.*
import kotlin.random.Random
import kotlin.random.asJavaRandom

class HeatSensor : CoroutineVerticle() {
  private val sensorId: String = UUID.randomUUID().toString()
  private var temperature: Double = 21.0

  private fun scheduleNextUpdate() {
    vertx.setTimer(Random.nextInt(5000) + 1000L) { update() }
  }

  private fun update() {
    temperature += (delta() / 10)
    scheduleNextUpdate()
  }

  private fun delta(): Double =
    if (Random.nextInt() > 0) Random.asJavaRandom().nextGaussian()
    else -Random.asJavaRandom().nextGaussian()

  override suspend fun start() {
    vertx.createHttpServer()
      .requestHandler(::handleRequest)
      .listen(config.getInteger("http.port", 3000))
      .await()
    scheduleNextUpdate()
  }

  private fun handleRequest(req: HttpServerRequest) {
    val data = json { obj("id" to sensorId, "temp" to temperature) }
    req.response()
      .putHeader("Content-Type", "application/json")
      .end(data.encode())
  }
}