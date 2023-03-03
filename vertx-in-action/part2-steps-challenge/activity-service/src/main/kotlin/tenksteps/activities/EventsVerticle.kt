package tenksteps.activities

import io.vertx.core.json.JsonObject
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.consumer.KafkaConsumerRecord
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgException
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import mu.KotlinLogging
import tenksteps.activities.SqlQueries.insertStepEvent
import tenksteps.activities.SqlQueries.stepsCountForToday
import utilities.core.setTimer
import utilities.core.streams.handler
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

class EventsVerticle : CoroutineVerticle() {
  private lateinit var eventConsumer: KafkaConsumer<String, JsonObject>
  private lateinit var updateProducer: KafkaProducer<String, JsonObject>
  private lateinit var pgPool: PgPool

  override suspend fun start() {
    eventConsumer = KafkaConsumer.create(vertx, kafkaConsumerConfig("activity-service"))
    updateProducer = KafkaProducer.create(vertx, kafkaProducerConfig)
    pgPool = PgPool.pool(vertx, pgConnectOptions, poolOptionsOf())

    eventConsumer.subscribe("incoming.steps").await()
    eventConsumer.handler(this) { record ->
      try {
        handleRecord(record)
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
        vertx.setTimer(this@EventsVerticle, 12.seconds) { handleRecord(record) }
      }
    }
  }

  private suspend fun handleRecord(record: KafkaConsumerRecord<String, JsonObject>) {
    insertRecord(record)
    generateActivityUpdate(record)
    commitKafkaConsumerOffset()
  }

  private suspend fun insertRecord(record: KafkaConsumerRecord<String, JsonObject>) {
    val data = record.value()
    val values =
      Tuple.of(data.getString("deviceId"), data.getLong("deviceSync"), data.getInteger("stepsCount"))
    try {
      pgPool.preparedQuery(insertStepEvent)
        .execute(values)
        .await()
    } catch (e: Throwable) {
      if (!duplicateKeyInsert(e)) throw e
    }
  }

  private fun duplicateKeyInsert(e: Throwable) = (e is PgException) && "23505" == e.code

  private suspend fun generateActivityUpdate(record: KafkaConsumerRecord<String, JsonObject>) {
    val deviceId = record.value().getString("deviceId")
    val now = LocalDateTime.now()
    val key = "$deviceId:${now.year}-${now.month}-${now.dayOfMonth}"
    val row = pgPool.preparedQuery(stepsCountForToday)
      .execute(Tuple.of(deviceId))
      .await()
      .first()
    val json = jsonObjectOf(
      "deviceId" to deviceId,
      "timestamp" to row.getTemporal(0).toString(),
      "stepsCount" to row.getLong(1)
    )
    updateProducer.send(KafkaProducerRecord.create("daily.step.updates", key, json)).await()
  }

  private suspend fun commitKafkaConsumerOffset() {
    eventConsumer.commit().await()
  }
}