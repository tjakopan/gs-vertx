package chapter03

import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(VertxExtension::class)
class SensorDataTest {
  @Test
  fun testAverage(vertx: Vertx) = runTest {
    val bus = vertx.eventBus()
    vertx.deployVerticle(SensorData()).await()
    bus.publish("sensor.updates", json { obj("id" to "a", "temp" to 20.0) })
    bus.publish("sensor.updates", json { obj("id" to "b", "temp" to 22.0) })

    val reply = bus.request<JsonObject>("sensor.average", "").await()

    reply.body().getDouble("average") shouldBe 21.0
  }
}