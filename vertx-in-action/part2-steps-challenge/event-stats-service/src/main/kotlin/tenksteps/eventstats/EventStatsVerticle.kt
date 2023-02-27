package tenksteps.eventstats

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.consumer.KafkaConsumerRecord
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import utilities.core.setPeriodic
import utilities.core.setTimer
import utilities.core.streams.handler
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

class EventStatsVerticle : CoroutineVerticle() {
  private lateinit var webClient: WebClient
  private lateinit var kafkaProducer: KafkaProducer<String, JsonObject>

  override suspend fun start() {
    webClient = WebClient.create(vertx)
    kafkaProducer = KafkaProducer.create(vertx, kafkaProducerConfig)

    handleIncomingSteps()
    handleDailyStepUpdates()
    handleEventStatsUserActivityUpdates()
  }

  private suspend fun handleIncomingSteps() {
    val kafkaConsumerRecords: MutableList<KafkaConsumerRecord<String, JsonObject>> = mutableListOf()
    val kafkaConsumer = KafkaConsumer.create<String, JsonObject>(vertx, kafkaConsumerConfig("event-stats-throughput"))
    kafkaConsumer.subscribe("incoming.steps").await()
    kafkaConsumer.handler { record -> kafkaConsumerRecords.add(record) }
    vertx.setPeriodic(this, 5.seconds) {
      val records = ArrayList(kafkaConsumerRecords)
      kafkaConsumerRecords.clear()
      try {
        publishThroughput(records)
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
        retryLater { publishThroughput(records) }
      }
    }
  }

  private suspend fun publishThroughput(kafkaConsumerRecords: List<KafkaConsumerRecord<String, JsonObject>>) {
    val kafkaProducerRecord = KafkaProducerRecord.create<String, JsonObject>(
      "event-stats.throughput",
      jsonObjectOf(
        "seconds" to 5,
        "count" to kafkaConsumerRecords.size,
        "throughput" to kafkaConsumerRecords.size / 5.0
      )
    )
    kafkaProducer.write(kafkaProducerRecord).await()
  }

  private suspend fun handleDailyStepUpdates() {
    val kafkaConsumer =
      KafkaConsumer.create<String, JsonObject>(vertx, kafkaConsumerConfig("event-stats-user-activity-updates"))
    kafkaConsumer.subscribe("daily.step.updates").await()
    kafkaConsumer.handler(this) { record ->
      try {
        var data = addDeviceOwner(record)
        data = addOwnerData(data)
        publishUserActivityUpdate(data)
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
        retryLater {
          var data = addDeviceOwner(record)
          data = addOwnerData(data)
          publishUserActivityUpdate(data)
        }
      }
    }
  }

  private suspend fun addDeviceOwner(record: KafkaConsumerRecord<String, JsonObject>): JsonObject {
    val data = record.value()
    val body = webClient.get(3000, "localhost", "/owns/${data.getString("deviceId")}")
      .`as`(BodyCodec.jsonObject())
      .send()
      .await()
      .body()
    return data.mergeIn(body)
  }

  private suspend fun addOwnerData(data: JsonObject): JsonObject {
    val username = data.getString("username")
    val body = webClient.get(3000, "localhost", "/$username")
      .`as`(BodyCodec.jsonObject())
      .send()
      .await()
      .body()
    return data.mergeIn(body)
  }

  private suspend fun publishUserActivityUpdate(data: JsonObject) {
    kafkaProducer.write(
      KafkaProducerRecord.create(
        "event-stats.user-activity.updates",
        data.getString("username"),
        data
      )
    ).await()
  }

  private suspend fun handleEventStatsUserActivityUpdates() {
    val kafkaConsumerRecordsPerCity = mutableMapOf<String, MutableList<KafkaConsumerRecord<String, JsonObject>>>()
    val kafkaConsumer = KafkaConsumer.create<String, JsonObject>(vertx, kafkaConsumerConfig("event-stats-city-trends"))
    kafkaConsumer.subscribe("event-stats.user-activity.updates").await()
    kafkaConsumer.handler { record ->
      val city = city(record)
      kafkaConsumerRecordsPerCity.merge(city, mutableListOf(record)) { l1, l2 ->
        l1.addAll(l2)
        l1
      }
    }
    vertx.setPeriodic(this, 5.seconds) {
      val recordsPerCity =
        HashMap<String, MutableList<KafkaConsumerRecord<String, JsonObject>>>(kafkaConsumerRecordsPerCity)
      kafkaConsumerRecordsPerCity.clear()
      try {
        publishCityTrendUpdate(recordsPerCity)
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
        retryLater { publishCityTrendUpdate(recordsPerCity) }
      }
    }
  }

  private fun city(record: KafkaConsumerRecord<String, JsonObject>): String = record.value().getString("city")

  private suspend fun publishCityTrendUpdate(recordsPerCity: Map<String, MutableList<KafkaConsumerRecord<String, JsonObject>>>) {
    recordsPerCity.forEach { (city, records) ->
      if (records.size > 0) {
        val stepsCount = records.sumOf { record -> record.value().getLong("stepsCount") }
        val record = KafkaProducerRecord.create(
          "event-stats.city-trend.updates",
          city,
          jsonObjectOf(
            "timestamp" to LocalDateTime.now().toString(),
            "seconds" to 5,
            "city" to city,
            "stepsCount" to stepsCount,
            "updates" to records.size
          )
        )
        kafkaProducer.write(record).await()
      }
    }
  }

  private suspend fun retryLater(action: suspend () -> Unit) {
    vertx.setTimer(this, 10.seconds) { action() }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      try {
        Vertx.vertx().deployVerticle(EventStatsVerticle()).await()
        logger.info { "Started" }
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
      }
    }
  }
}