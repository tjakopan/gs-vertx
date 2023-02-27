package tenksteps.congrats

import io.kotest.assertions.timing.continually
import io.kotest.assertions.timing.eventually
import io.kotest.assertions.until.fixed
import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.mail.MailClient
import io.vertx.ext.mail.MailConfig
import io.vertx.ext.mail.MailMessage
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.junit5.VertxExtension
import io.vertx.kafka.admin.KafkaAdminClient
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import io.vertx.kafka.client.serialization.JsonObjectSerializer
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.time.LocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(VertxExtension::class)
@DisplayName("Tests for the congrats service")
@Testcontainers
class CongratsTest {
  companion object {
    @Suppress("unused")
    @Container
    @JvmStatic
    private val containers = DockerComposeContainer(File("../docker-compose.yml")).apply {
      withExposedService("kafka_1", 19092)
      withExposedService("mailhog_1", 8025)
    }
  }

  private lateinit var kafkaProducer: KafkaProducer<String, JsonObject>
  private lateinit var webClient: WebClient

  @BeforeTest
  fun setUp(vertx: Vertx): Unit = runTest {
    val kafkaConf = mapOf(
      "bootstrap.servers" to "localhost:19092",
      "key.serializer" to StringSerializer::class.java.name,
      "value.serializer" to JsonObjectSerializer::class.java.name,
      "acks" to "1"
    )
    kafkaProducer = KafkaProducer.create(vertx, kafkaConf)
    webClient = WebClient.create(vertx)
    val kafkaAdminClient = KafkaAdminClient.create(vertx, kafkaConf)
    try {
      kafkaAdminClient.deleteTopics(listOf("incoming.steps", "daily.step.updates")).await()
    } catch (ignored: UnknownTopicOrPartitionException) {
    }
    vertx.deployVerticle(CongratsVerticle()).await()
    vertx.deployVerticle(FakeUserService()).await()
    webClient.delete(8025, "localhost", "/api/v1/messages").send().await()
  }

  private fun kafkaRecord(deviceId: String, steps: Long): KafkaProducerRecord<String, JsonObject> {
    val now = LocalDateTime.now()
    val key = "$deviceId:${now.year}-${now.month}-${now.dayOfMonth}"
    val json = jsonObjectOf("deviceId" to deviceId, "timestamp" to now.toString(), "stepsCount" to steps)
    return KafkaProducerRecord.create("daily.step.updates", key, json)
  }

  @Test
  fun `smoke test to send a mail using mailhog`(vertx: Vertx) = runTest {
    val config = MailConfig().setHostname("localhost").setPort(1025)
    val client = MailClient.createShared(vertx, config)
    val message = MailMessage()
      .setFrom("a@b.tld")
      .setSubject("Yo")
      .setTo("c@tld")
      .setText("This is cool")
    client.sendMail(message).await()
  }

  @Test
  fun `no email must be sent below 10k steps`() = runTest {
    kafkaProducer.send(kafkaRecord("123", 5000)).await()

    continually(duration = 3.seconds, interval = 200.milliseconds.fixed()) {
      val json = webClient.get(8025, "localhost", "/api/v2/search?kind=to&query=foo@mail.tld")
        .`as`(BodyCodec.jsonObject())
        .send()
        .await()
        .body()
      json.getInteger("total") shouldBe 0
    }
  }

  @Test
  fun `an email must be sent for 10k+ steps`() = runTest {
    kafkaProducer.send(kafkaRecord("123", 11_000)).await()

    eventually(duration = 3.seconds) {
      val json = webClient.get(8025, "localhost", "/api/v2/search?kind=to&query=foo@mail.tld")
        .`as`(BodyCodec.jsonObject())
        .send()
        .await()
        .body()
      json.getInteger("total") shouldBe 1
    }
  }

  @Test
  fun `just one email must be sent to a user for 10k+ steps on single day`() = runTest {
    kafkaProducer.send(kafkaRecord("123", 11_000)).await()

    eventually(duration = 3.seconds) {
      val json = webClient.get(8025, "localhost", "/api/v2/search?kind=to&query=foo@mail.tld")
        .`as`(BodyCodec.jsonObject())
        .send()
        .await()
        .body()
      json.getInteger("total") shouldBe 1
    }

    kafkaProducer.send(kafkaRecord("123", 11_000)).await()

    continually(duration = 3.seconds, interval = 200.milliseconds.fixed()) {
      val json = webClient.get(8025, "localhost", "/api/v2/search?kind=to&query=foo@mail.tld")
        .`as`(BodyCodec.jsonObject())
        .send()
        .await()
        .body()
      json.getInteger("total") shouldBe 1
    }
  }
}