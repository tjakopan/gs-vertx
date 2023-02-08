package chapter06

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.shouldBeWithinPercentageOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.await
import io.vertx.serviceproxy.ServiceException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(VertxExtension::class)
class SensorDataServiceTest {
  private lateinit var dataService: SensorDataService

  @BeforeTest
  fun prepare(vertx: Vertx) = runTest {
    vertx.deployVerticle(DataVerticle()).await()
    dataService = createSensorDataServiceProxy(vertx, "sensor.data-service")
  }

  @Test
  fun noSensor() = runTest {
    val action = suspend { dataService.valueFor("abc").await() }
    val avg = dataService.average().await().getDouble("average")

    shouldThrow<ServiceException> { action() }
      .message shouldStartWith "No value has been observed"
    avg.shouldBeWithinPercentageOf(0.0, 1.0)
  }

  @Test
  fun withSensors(vertx: Vertx) = runTest {
    val m1 = json { obj("id" to "abc", "temp" to 21.0) }
    val m2 = json { obj("id" to "def", "temp" to 23.0) }
    vertx.eventBus()
      .publish("sensor.updates", m1)
      .publish("sensor.updates", m2)

    val val1 = dataService.valueFor("abc").await()
    val avg = dataService.average().await()

    val1.getString("sensorId") shouldBe "abc"
    val1.getDouble("value") shouldBe 21.0
    avg.getDouble("average").shouldBeWithinPercentageOf(22.0, 1.0)
  }
}