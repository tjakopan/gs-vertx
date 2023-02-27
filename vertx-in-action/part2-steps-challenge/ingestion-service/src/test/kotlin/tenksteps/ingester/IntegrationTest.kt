package tenksteps.ingester

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.specification.RequestSpecification
import io.vertx.amqp.AmqpClient
import io.vertx.amqp.AmqpClientOptions
import io.vertx.amqp.AmqpMessage
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.kafka.admin.KafkaAdminClient
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.serialization.JsonObjectDeserializer
import io.vertx.kotlin.amqp.amqpClientOptionsOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import org.apache.kafka.common.serialization.StringDeserializer
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
@ExtendWith(VertxExtension::class)
@Testcontainers
class IntegrationTest {
  companion object {
    @Suppress("unused")
    @Container
    @JvmStatic
    private val containers = DockerComposeContainer(File("../docker-compose.yml")).apply {
      withExposedService("activemq_1", 5672)
      withExposedService("kafka_1", 19092)
    }
  }

  private val requestSpecification: RequestSpecification = RequestSpecBuilder()
    .addFilters(listOf(ResponseLoggingFilter(), RequestLoggingFilter()))
    .setBaseUri("http://localhost:3002")
    .build()

  private fun kafkaConfig(): Map<String, String> = mapOf(
    "bootstrap.servers" to "localhost:19092",
    "key.deserializer" to StringDeserializer::class.java.name,
    "value.deserializer" to JsonObjectDeserializer::class.java.name,
    "auto.offset.reset" to "earliest",
    "enable.auto.commit" to "false",
    "group.id" to "ingester-test-" + System.currentTimeMillis()
  )

  private lateinit var kafkaConsumer: KafkaConsumer<String, JsonObject>

  private val amqpClientOptions: AmqpClientOptions =
    amqpClientOptionsOf(host = "localhost", port = 5672, username = "artemis", password = "artemis")

  private lateinit var amqpClient: AmqpClient

  @BeforeTest
  fun setUp(vertx: Vertx): Unit = runTest {
    kafkaConsumer = KafkaConsumer.create(vertx, kafkaConfig())
    amqpClient = AmqpClient.create(vertx, amqpClientOptions)
    val kafkaAdminClient = KafkaAdminClient.create(vertx, kafkaConfig())
    vertx.deployVerticle(IngesterVerticle()).await()
    try {
      kafkaAdminClient.deleteTopics(listOf("incoming.steps")).await()
    } catch (ignored: UnknownTopicOrPartitionException) {
    }
  }

  @Test
  fun `ingest a well-formed AMQP message`() = runTest {
    val body = jsonObjectOf("deviceId" to "123", "deviceSync" to 1L, "stepsCount" to 500)
    val amqpConn = amqpClient.connect().await()
    val amqpSender = amqpConn.createSender("step-events").await()
    val amqpMessage = AmqpMessage.create()
      .durable(true)
      .ttl(500)
      .withJsonObjectAsBody(body)
      .build()
    kafkaConsumer.subscribe("incoming.steps").await()

    amqpSender.send(amqpMessage)

    val kafkaRecord = kafkaConsumer.poll(3.seconds.toJavaDuration()).await().recordAt(0)
    kafkaRecord.key() shouldBe "123"
    val json = kafkaRecord.value()
    json.getString("deviceId") shouldBe "123"
    json.getLong("deviceSync") shouldBe 1L
    json.getInteger("stepsCount") shouldBe 500
  }

  @Test
  fun `ingest a badly-formed AMQP message and observe no Kafka record`() = runTest {
    val body = jsonObjectOf()
    val amqpConn = amqpClient.connect().await()
    val amqpSender = amqpConn.createSender("step-events").await()
    val amqpMessage = AmqpMessage.create()
      .durable(true)
      .ttl(500)
      .withJsonObjectAsBody(body)
      .build()
    kafkaConsumer.subscribe("incoming.steps").await()

    amqpSender.send(amqpMessage)

    val kafkaRecords = kafkaConsumer.poll(3.seconds.toJavaDuration()).await()
    kafkaRecords.isEmpty.shouldBeTrue()
  }

  @Test
  fun `ingest a well-formed JSON data over HTTP`() = runTest {
    val body = jsonObjectOf("deviceId" to "456", "deviceSync" to 3L, "stepsCount" to 125)
    kafkaConsumer.subscribe("incoming.steps").await()

    Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      body(body.encode())
    } When {
      post("/ingest")
    } Then {
      statusCode(200)
    }

    val kafkaRecord = kafkaConsumer.poll(3.seconds.toJavaDuration()).await().recordAt(0)
    kafkaRecord.key() shouldBe "456"
    val json = kafkaRecord.value()
    json.getString("deviceId") shouldBe "456"
    json.getLong("deviceSync") shouldBe 3L
    json.getInteger("stepsCount") shouldBe 125
  }

  @Test
  fun `ingest a badly-formed JSON data over HTTP and observe no Kafka record`() = runTest {
    val body = jsonObjectOf()
    kafkaConsumer.subscribe("incoming.steps").await()

    Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      body(body.encode())
    } When {
      post("/ingest")
    } Then {
      statusCode(400)
    }

    val kafkaRecords = kafkaConsumer.poll(3.seconds.toJavaDuration()).await()
    kafkaRecords.isEmpty.shouldBeTrue()
  }
}