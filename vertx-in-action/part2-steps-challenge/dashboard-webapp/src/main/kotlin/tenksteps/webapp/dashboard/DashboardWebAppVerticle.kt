package tenksteps.webapp.dashboard

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.consumer.KafkaConsumerRecord
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.ext.bridge.permittedOptionsOf
import io.vertx.kotlin.ext.web.handler.sockjs.sockJSBridgeOptionsOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

private const val HTTP_PORT = 8081
private val logger = KotlinLogging.logger { }

class DashboardWebAppVerticle : CoroutineVerticle() {
  private val publicRanking: MutableMap<String, JsonObject> = mutableMapOf()

  override suspend fun start() {
    val router = Router.router(vertx)

    val kafkaConsumerThroughput =
      KafkaConsumer.create<String, JsonObject>(vertx, kafkaConsumerConfig("dashboard-webapp-throughput"))
    kafkaConsumerThroughput.subscribe("event-stats.throughput").await()
    kafkaConsumerThroughput.handler { record -> forwardKafkaRecord(record, "client.updates.throughput") }

    val kafkaConsumerCityTrends =
      KafkaConsumer.create<String, JsonObject>(vertx, kafkaConsumerConfig("dashboard-webapp-city-trend"))
    kafkaConsumerCityTrends.subscribe("event-stats.city-trend.updates").await()
    kafkaConsumerThroughput.handler { record -> forwardKafkaRecord(record, "client.updates.city-trend") }

    val userActivityRecords: MutableList<KafkaConsumerRecord<String, JsonObject>> = mutableListOf()
    val kafkaConsumerUserActivities =
      KafkaConsumer.create<String, JsonObject>(vertx, kafkaConsumerConfig("dashboard-webapp-ranking"))
    kafkaConsumerUserActivities.subscribe("event-stats.user-activity.updates").await()
    kafkaConsumerUserActivities.handler { record ->
      if (record.value().getBoolean("makePublic")) userActivityRecords.add(record)
    }
    vertx.setPeriodic(5.seconds.inWholeMilliseconds) {
      val records = ArrayList(userActivityRecords)
      userActivityRecords.clear()
      updatePublicRanking(records)
    }

    hydrate()

    val sockJsHandler = SockJSHandler.create(vertx)
    val sockJsBridgeOptions = sockJSBridgeOptionsOf(
      inboundPermitteds = listOf(permittedOptionsOf(addressRegex = "client.updates.*")),
      outboundPermitteds = listOf(permittedOptionsOf(addressRegex = "client.updates.*"))
    )
    sockJsHandler.bridge(sockJsBridgeOptions)
    router.route("/eventbus/*").handler(sockJsHandler)

    router.route().handler(StaticHandler.create("webroot/assets"))
    router.get("/*").handler { ctx -> ctx.reroute("/index.html") }

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(HTTP_PORT)
      .await()
  }

  private fun forwardKafkaRecord(record: KafkaConsumerRecord<String, JsonObject>, destination: String) {
    vertx.eventBus().publish(destination, record.value())
  }

  private fun updatePublicRanking(records: List<KafkaConsumerRecord<String, JsonObject>>) {
    copyBetterScores(records)
    pruneOldEntries()
    vertx.eventBus().publish("client.updates.publicRanking", computeRanking())
  }

  private fun copyBetterScores(records: List<KafkaConsumerRecord<String, JsonObject>>) {
    records.forEach { record ->
      val json = record.value()
      val stepsCount = json.getLong("stepsCount")
      val username = json.getString("username")
      val previousData = publicRanking[username]
      if (previousData == null || previousData.getLong("stepsCount") < stepsCount) {
        publicRanking[username] = json
      }
    }
  }

  private fun pruneOldEntries() {
    val now = Instant.now()
    val iterator = publicRanking.entries.iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      val timestamp = Instant.parse(entry.value.getString("timestamp"))
      if (timestamp.until(now, ChronoUnit.DAYS) >= 1) {
        iterator.remove()
      }
    }
  }

  private fun computeRanking(): JsonArray {
    val ranking = publicRanking.entries
      .map { entry -> entry.value }
      .sortedWith(::compareStepsCountInReverseOrder)
      .map { json ->
        jsonObjectOf(
          "username" to json.getString("username"),
          "stepsCount" to json.getLong("stepsCount"),
          "city" to json.getString("city")
        )
      }
    return JsonArray(ranking)
  }

  private fun compareStepsCountInReverseOrder(a: JsonObject, b: JsonObject): Int {
    val first = a.getLong("stepsCount")
    val second = b.getLong("stepsCount")
    return second.compareTo(first)
  }

  private suspend fun hydrate() {
    val webClient = WebClient.create(vertx)
    val jsonArray = retry(5) {
      val body = webClient.get(3001, "localhost", "/ranking-last-24-hours")
        .`as`(BodyCodec.jsonArray())
        .send()
        .await()
        .body()
      delay(5.seconds)
      body
    }
    jsonArray.forEach { json ->
      try {
        var jsonObject = json as JsonObject
        jsonObject = whoOwnsDevice(webClient, jsonObject)
        jsonObject = fillWithUserProfile(webClient, jsonObject)
        hydrateEntryIfPublic(jsonObject)
        logger.info { "Hydration completed" }
      } catch (e: Throwable) {
        logger.error(e) { "Hydration error" }
      }
    }
  }

  private suspend fun whoOwnsDevice(webClient: WebClient, json: JsonObject): JsonObject {
    val body = retry(5) {
      webClient.get(3000, "localhost", "/owns/${json.getString("deviceId")}")
        .`as`(BodyCodec.jsonObject())
        .send()
        .await()
        .body()
    }
    return body.mergeIn(json)
  }

  private suspend fun fillWithUserProfile(webClient: WebClient, json: JsonObject): JsonObject {
    val body = retry(5) {
      webClient.get(3000, "localhost", "/${json.getString("username")}")
        .`as`(BodyCodec.jsonObject())
        .send()
        .await()
        .body()
    }
    return body.mergeIn(json)
  }

  private fun hydrateEntryIfPublic(data: JsonObject) {
    if (data.getBoolean("makePublic")) {
      data.put("timestamp", Instant.now().toString())
      publicRanking[data.getString("username")] = data
    }
  }

  private suspend fun <T> retry(count: Int, action: suspend () -> T): T {
    for (i in 1..count) {
      try {
        return action()
      } catch (e: Throwable) {
        if (i == count) throw e
      }
    }
    return null!!
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      try {
        Vertx.vertx().deployVerticle(DashboardWebAppVerticle()).await()
        logger.info { "HTTP server started on port $HTTP_PORT" }
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
      }
    }
  }
}