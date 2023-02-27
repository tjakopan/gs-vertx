package tenksteps.congrats

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.AsyncMap
import io.vertx.ext.mail.MailClient
import io.vertx.ext.mail.MailMessage
import io.vertx.ext.mail.MailResult
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.consumer.KafkaConsumerRecord
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import utilities.core.setTimer
import utilities.core.streams.handler
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

class CongratsVerticle : CoroutineVerticle() {
  private lateinit var mailClient: MailClient
  private lateinit var webClient: WebClient
  private lateinit var kafkaKeys: AsyncMap<String, Boolean>

  override suspend fun start() {
    mailClient = MailClient.createShared(vertx, mailerConfig)
    webClient = WebClient.create(vertx)
    kafkaKeys = vertx.sharedData().getLocalAsyncMap<String, Boolean>("kafka-keys").await()

    val kafkaConsumer = KafkaConsumer.create<String, JsonObject>(vertx, kafkaConsumerConfig("congrats-service"))
    kafkaConsumer.subscribe("daily.step.updates").await()
    kafkaConsumer.handler(this) { record ->
      try {
        handleKafkaRecord(record)
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
        vertx.setTimer(this@CongratsVerticle, 10.seconds) {
          handleKafkaRecord(record)
        }
      }
    }
  }

  private suspend fun handleKafkaRecord(record: KafkaConsumerRecord<String, JsonObject>) {
    if (record.value().getInteger("stepsCount") < 10_000) return
    val keyProcessed = kafkaKeys.get(record.key()).await() ?: false
    if (keyProcessed) return
    val mailResult = sendMail(record)
    kafkaKeys.put(record.key(), true, 24.hours.inWholeMilliseconds)
    logger.info { "Congratulated ${mailResult.recipients}" }
  }

  private suspend fun sendMail(record: KafkaConsumerRecord<String, JsonObject>): MailResult {
    val deviceId = record.value().getString("deviceId")
    val stepsCount = record.value().getInteger("stepsCount")
    val json = webClient.get(3000, "localhost", "/owns/$deviceId")
      .`as`(BodyCodec.jsonObject())
      .send()
      .await()
      .body()
    val username = json.getString("username")
    val email = getEmail(username)
    val msg = makeEmail(stepsCount, email)
    return mailClient.sendMail(msg).await()
  }

  private suspend fun getEmail(username: String): String =
    webClient.get(3000, "localhost", "/$username")
      .`as`(BodyCodec.jsonObject())
      .send()
      .await()
      .body()
      .getString("email")

  private fun makeEmail(stepsCount: Int, email: String) =
    MailMessage()
      .setFrom("noreply@tenksteps.tld")
      .setTo(email)
      .setSubject("You made it!")
      .setText("Congratulations on reaching $stepsCount steps today!\n\n- The 10k steps team\n")

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      Vertx.vertx().deployVerticle(CongratsVerticle()).await()
      logger.info { "Ready to notify people reaching more than 10k steps" }
    }
  }
}