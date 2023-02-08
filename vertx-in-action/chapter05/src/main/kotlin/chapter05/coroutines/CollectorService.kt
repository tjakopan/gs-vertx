package chapter05.coroutines

import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.predicate.ResponsePredicate
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.async
import mu.KotlinLogging
import utilities.core.http.requestHandler

private val logger = KotlinLogging.logger { }

class CollectorService : CoroutineVerticle() {
  private lateinit var webClient: WebClient

  override suspend fun start() {
    webClient = WebClient.create(vertx)
    vertx.createHttpServer()
      .requestHandler(this) { req -> handleRequest(req) }
      .listen(8080)
      .await()
  }

  private suspend fun handleRequest(req: HttpServerRequest) {
    try {
      val t1 = async { fetchTemperature(3000) }
      val t2 = async { fetchTemperature(3001) }
      val t3 = async { fetchTemperature(3002) }

      val array = jsonArrayOf(t1.await(), t2.await(), t3.await())
      val json = json { obj("data" to array) }

      sendToSnapshot(json)
      req.response()
        .putHeader("Content-Type", "application/json")
        .end(json.encode())
    } catch (e: Throwable) {
      logger.error(e) { "Something went wrong" }
      req.response().setStatusCode(500).end()
    }
  }

  private suspend fun fetchTemperature(port: Int): JsonObject =
    webClient.get(port, "localhost", "/")
      .expect(ResponsePredicate.SC_SUCCESS)
      .`as`(BodyCodec.jsonObject())
      .send()
      .await()
      .body()

  private suspend fun sendToSnapshot(json: JsonObject) {
    webClient.post(4000, "localhost", "/")
      .expect(ResponsePredicate.SC_SUCCESS)
      .sendJson(json)
      .await()
  }
}