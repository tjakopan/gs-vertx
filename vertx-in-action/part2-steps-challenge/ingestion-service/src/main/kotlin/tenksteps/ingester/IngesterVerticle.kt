package tenksteps.ingester

import io.vertx.amqp.AmqpClient
import io.vertx.amqp.AmqpClientOptions
import io.vertx.amqp.AmqpMessage
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import io.vertx.kafka.client.serialization.JsonObjectSerializer
import io.vertx.kotlin.amqp.amqpClientOptionsOf
import io.vertx.kotlin.amqp.amqpReceiverOptionsOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.kafka.common.serialization.StringSerializer
import utilities.core.setTimer
import utilities.core.streams.handler
import utilities.web.handler
import kotlin.time.Duration.Companion.seconds

private const val HTTP_PORT = 3002
private val logger = KotlinLogging.logger { }

class IngesterVerticle : CoroutineVerticle() {
  private lateinit var updateProducer: KafkaProducer<String, JsonObject>

  override suspend fun start() {
    updateProducer = KafkaProducer.create(vertx, kafkaConfig)

    val amqpConn = AmqpClient.create(vertx, amqpConfig)
      .connect()
      .await()
    amqpConn.createReceiver("step-events", amqpReceiverOptionsOf(autoAcknowledgement = false, durable = true))
      .await()
      .handler(this) { message ->
        try {
          handleAmqpMessage(message)
        } catch (e: Throwable) {
          logger.error(e) { "Woops" }
          vertx.setTimer(this@IngesterVerticle, 10.seconds) {
            handleAmqpMessage(message)
          }
        }
      }

    val router = Router.router(vertx)
    router.post().handler(BodyHandler.create())
    router.post("/ingest").handler(this) { ctx -> httpIngest(ctx) }

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(HTTP_PORT)
      .await()
  }

  private val kafkaConfig: Map<String, String> = mapOf(
    "bootstrap.servers" to "localhost:19092",
    "key.serializer" to StringSerializer::class.java.name,
    "value.serializer" to JsonObjectSerializer::class.java.name,
    "acks" to "1"
  )

  private val amqpConfig: AmqpClientOptions = amqpClientOptionsOf(
    host = "localhost",
    port = 5672,
    username = "artemis",
    password = "artemis"
  )

  private suspend fun handleAmqpMessage(message: AmqpMessage) {
    if ("application/json" != message.contentType() || invalidIngestedJson(message.bodyAsJsonObject())) {
      logger.error { "Invalid AMQP message (discarded): ${message.bodyAsBinary()}" }
      message.accepted()
      return
    }

    val payload = message.bodyAsJsonObject()
    val kafkaRecord = makeKafkaRecord(payload)
    try {
      updateProducer.send(kafkaRecord).await()
      message.accepted()
    } catch (e: Throwable) {
      logger.error(e) { "AMQP ingestion failed" }
      message.rejected()
    }
  }

  private suspend fun httpIngest(ctx: RoutingContext) {
    val payload = ctx.body().asJsonObject()
    if (invalidIngestedJson(payload)) {
      logger.error { "Invalid HTTP json (discarded): ${payload.encode()}" }
      ctx.fail(400)
      return
    }

    val kafkaRecord = makeKafkaRecord(payload)
    try {
      updateProducer.send(kafkaRecord).await()
      ctx.response().end()
    } catch (e: Throwable) {
      logger.error(e) { "HTTP ingestion failed" }
      ctx.fail(500)
    }
  }

  private fun invalidIngestedJson(payload: JsonObject): Boolean =
    !payload.containsKey("deviceId") || !payload.containsKey("deviceSync") || !payload.containsKey("stepsCount")

  private fun makeKafkaRecord(payload: JsonObject): KafkaProducerRecord<String, JsonObject> {
    val deviceId = payload.getString("deviceId")
    val recordData = jsonObjectOf(
      "deviceId" to deviceId,
      "deviceSync" to payload.getLong("deviceSync"),
      "stepsCount" to payload.getInteger("stepsCount")
    )
    return KafkaProducerRecord.create("incoming.steps", deviceId, recordData)
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      try {
        Vertx.vertx().deployVerticle(IngesterVerticle()).await()
        logger.info { "HTTP server started on port $HTTP_PORT" }
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
      }
    }
  }
}