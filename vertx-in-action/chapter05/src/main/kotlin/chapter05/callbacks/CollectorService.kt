package chapter05.callbacks

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
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

  override fun start() {
    webClient = WebClient.create(vertx)
    vertx.createHttpServer()
      .requestHandler(::handleRequest)
      .listen(8080)
  }

  private fun handleRequest(req: HttpServerRequest) {
    val responses = mutableListOf<JsonObject>()
    var counter = 0
    for (i in 0..2) {
      webClient.get(3000 + i, "localhost", "/")
        .expect(ResponsePredicate.SC_SUCCESS)
        .`as`(BodyCodec.jsonObject())
        .send { ar ->
          if (ar.succeeded()) responses.add(ar.result().body())
          else logger.error(ar.cause()) { "Sensor down?" }
          if (++counter == 3) {
            val data = json { obj("data" to array(responses)) }
            sendToSnapshot(req, data)
          }
        }
    }
  }

  private fun sendToSnapshot(req: HttpServerRequest, data: JsonObject) {
    webClient.post(4000, "localhost", "/")
      .expect(ResponsePredicate.SC_SUCCESS)
      .sendJsonObject(data) { ar ->
        if (ar.succeeded()) sendResponse(req, data)
        else {
          logger.error(ar.cause()) { "Snapshot down?" }
          req.response().setStatusCode(500).end()
        }
      }
  }

  private fun sendResponse(req: HttpServerRequest, data: JsonObject) {
    req.response()
      .putHeader("Content-Type", "application/json")
      .end(data.encode())
  }
}