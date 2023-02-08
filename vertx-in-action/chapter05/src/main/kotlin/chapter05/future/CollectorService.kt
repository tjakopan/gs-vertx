package chapter05.future

import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.predicate.ResponsePredicate
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class CollectorService : AbstractVerticle() {
  private lateinit var webClient: WebClient

  override fun start(promise: Promise<Void>) {
    webClient = WebClient.create(vertx)
    vertx.createHttpServer()
      .requestHandler(::handleRequest)
      .listen(8080)
      .onFailure(promise::fail)
      .onSuccess {
        logger.info { "http://localhost:8080/" }
        promise.complete()
      }
  }

  private fun handleRequest(req: HttpServerRequest) {
    CompositeFuture.all(
      fetchTemperature(3000),
      fetchTemperature(3001),
      fetchTemperature(3002)
    )
      .flatMap(::sendToSnapshot)
      .onSuccess { data ->
        req.response()
          .putHeader("Content-Type", "application/json")
          .end(data.encode())
      }
      .onFailure { e ->
        logger.error(e) { "Something went wrong" }
        req.response().setStatusCode(500).end()
      }
  }

  private fun sendToSnapshot(temps: CompositeFuture): Future<JsonObject> {
    val tempData = temps.list<JsonObject>()
    val data = json { obj("data" to array(tempData[0], tempData[1], tempData[2])) }
    return webClient.post(4000, "localhost", "/")
      .expect(ResponsePredicate.SC_SUCCESS)
      .sendJson(data)
      .map { data }
  }

  private fun fetchTemperature(port: Int): Future<JsonObject> {
    return webClient.get(port, "localhost", "/")
      .expect(ResponsePredicate.SC_SUCCESS)
      .`as`(BodyCodec.jsonObject())
      .send()
      .map(HttpResponse<JsonObject>::body)
  }
}