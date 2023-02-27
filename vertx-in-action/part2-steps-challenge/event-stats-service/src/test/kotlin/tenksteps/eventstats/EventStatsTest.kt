package tenksteps.eventstats

import io.kotest.assertions.asClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.shouldBeWithinPercentageOf
import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.kafka.admin.KafkaAdminClient
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.time.LocalDateTime
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(VertxExtension::class)
@DisplayName("Tests for the events-stats service")
@Testcontainers
class EventStatsTest {
  companion object {
    @Suppress("unused")
    @Container
    @JvmStatic
    private val containers = DockerComposeContainer(File("../docker-compose.yml")).apply {
      withExposedService("kafka_1", 19092)
    }
  }

  private lateinit var kafkaProducer: KafkaProducer<String, JsonObject>
  private lateinit var kafkaConsumer: KafkaConsumer<String, JsonObject>

  @BeforeTest
  fun setUp(vertx: Vertx): Unit = runTest {
    kafkaProducer = KafkaProducer.create(vertx, kafkaProducerConfig)
    kafkaConsumer = KafkaConsumer.create(vertx, kafkaConsumerConfig(UUID.randomUUID().toString()))
    val kafkaAdminClient = KafkaAdminClient.create(vertx, kafkaProducerConfig)
    try {
      kafkaAdminClient.deleteTopics(listOf("incoming.steps", "daily.step.updates")).await()
    } catch (ignored: UnknownTopicOrPartitionException) {
    }
    vertx.deployVerticle(EventStatsVerticle()).await()
    vertx.deployVerticle(FakeUserService()).await()
  }

  private fun dailyStepsUpdateRecord(deviceId: String, steps: Long): KafkaProducerRecord<String, JsonObject> {
    val now = LocalDateTime.now()
    val key = "$deviceId:${now.year}-${now.month}-${now.dayOfMonth}"
    val json = jsonObjectOf("deviceId" to deviceId, "timestamp" to now.toString(), "stepsCount" to steps)
    return KafkaProducerRecord.create("daily.step.updates", key, json)
  }

  private fun incomingStepsRecord(
    deviceId: String,
    syncId: Long,
    steps: Long
  ): KafkaProducerRecord<String, JsonObject> {
    val now = LocalDateTime.now()
    val key = "$deviceId:${now.year}-${now.month}-${now.dayOfMonth}"
    val json = jsonObjectOf("deviceId" to deviceId, "syncId" to syncId, "stepsCount" to steps)
    return KafkaProducerRecord.create("incoming.steps", key, json)
  }

  @Test
  fun `incoming activity throughput computation`() = runTest {
    for (i in 0..9) {
      kafkaProducer.send(incomingStepsRecord("abc", i.toLong(), 10))
    }
    kafkaConsumer.subscribe("event-stats.throughput").await()

    val data = kafkaConsumer.poll(10.seconds.toJavaDuration()).await().recordAt(0).value()

    data.asClue {
      it.getInteger("seconds") shouldBe 5
      it.getInteger("count") shouldBe 10
      it.getDouble("throughput").shouldBeWithinPercentageOf(2.0, 5.0)
    }
  }

  @Test
  fun `user activity updates`() = runTest {
    kafkaProducer.send(dailyStepsUpdateRecord("abc", 2500))
    kafkaConsumer.subscribe("event-stats.user-activity.updates").await()

    val data = kafkaConsumer.poll(10.seconds.toJavaDuration()).await().recordAt(0).value()

    data.asClue {
      it.getString("deviceId") shouldBe "abc"
      it.getString("username") shouldBe "Foo"
      it.getLong("stepsCount") shouldBe 2500
      it.containsKey("timestamp").shouldBeTrue()
      it.containsKey("city").shouldBeTrue()
      it.containsKey("makePublic").shouldBeTrue()
    }
  }

  @Test
  fun `city trend updates`() = runTest {
    kafkaProducer.send(dailyStepsUpdateRecord("abc", 2500))
    kafkaProducer.send(dailyStepsUpdateRecord("abc", 2500))
    kafkaConsumer.subscribe("event-stats.city-trend.updates").await()

    val data = kafkaConsumer.poll(10.seconds.toJavaDuration()).await().recordAt(0).value()

    data.asClue {
      it.getInteger("seconds") shouldBe 5
      it.getInteger("updates") shouldBe 2
      it.getLong("stepsCount") shouldBe 5000
      it.getString("city") shouldBe "Lyon"
    }
  }
}