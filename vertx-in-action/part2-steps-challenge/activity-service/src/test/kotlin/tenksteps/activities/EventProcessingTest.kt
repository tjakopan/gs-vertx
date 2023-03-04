package tenksteps.activities

import io.kotest.matchers.booleans.shouldBeTrue
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
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgPool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("SqlResolve", "SqlWithoutWhere")
@ExtendWith(VertxExtension::class)
@DisplayName("Kafka event processing tests")
@Testcontainers
class EventProcessingTest {
  companion object {
    @Suppress("unused")
    @Container
    @JvmStatic
    private val containers = DockerComposeContainer(File("../docker-compose.yml")).apply {
      withExposedService("postgres_1", 5432)
      withExposedService("mongo_1", 27017)
      withExposedService("kafka_1", 19092)
    }
  }

  private lateinit var consumer: KafkaConsumer<String, JsonObject>
  private lateinit var producer: KafkaProducer<String, JsonObject>

  @BeforeTest
  fun setUp(vertx: Vertx): Unit = runTest {
    consumer = KafkaConsumer.create(vertx, kafkaConsumerConfig("activity-service-test-${System.currentTimeMillis()}"))
    producer = KafkaProducer.create(vertx, kafkaProducerConfig)
    val kafkaAdminClient = KafkaAdminClient.create(vertx, kafkaProducerConfig)

    val pgPool = PgPool.pool(vertx, pgConnectOptions, poolOptionsOf())
    try {
      pgPool.query("delete from stepevent")
        .execute()
        .await()
      kafkaAdminClient.deleteTopics(listOf("incoming.steps", "daily.step.updates"))
    } finally {
      pgPool.close().await()
    }
  }

  @Test
  fun `send events from the same device and observe that a correct daily steps count event is being produced`(vertx: Vertx) =
    runTest {
      consumer.subscribe("daily.step.updates").await()
      vertx.deployVerticle(EventsVerticle()).await()

      var steps = jsonObjectOf("deviceId" to "123", "deviceSync" to 1L, "stepsCount" to 200)
      producer.send(KafkaProducerRecord.create("incoming.steps", "123", steps)).await()
      steps = jsonObjectOf("deviceId" to "123", "deviceSync" to 2L, "stepsCount" to 50)
      producer.send(KafkaProducerRecord.create("incoming.steps", "123", steps)).await()
      val record = consumer.poll(20.seconds.toJavaDuration()).await().recordAt(1)

      val json = record.value()
      json.getString("deviceId") shouldBe "123"
      json.containsKey("timestamp").shouldBeTrue()
      json.getInteger("stepsCount") shouldBe 250
    }
}