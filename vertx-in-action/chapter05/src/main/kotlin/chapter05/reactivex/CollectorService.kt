package chapter05.reactivex

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.obj
import io.vertx.rxjava3.core.AbstractVerticle
import io.vertx.rxjava3.core.http.HttpServerRequest
import io.vertx.rxjava3.ext.web.client.HttpResponse
import io.vertx.rxjava3.ext.web.client.WebClient
import io.vertx.rxjava3.ext.web.client.predicate.ResponsePredicate
import io.vertx.rxjava3.ext.web.codec.BodyCodec
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class CollectorService : AbstractVerticle() {
  private lateinit var webClient: WebClient

  override fun rxStart(): Completable {
    webClient = WebClient.create(vertx)
    return vertx.createHttpServer()
      .requestHandler(::handleRequest)
      .rxListen(8080)
      .ignoreElement()
  }

  private fun handleRequest(req: HttpServerRequest) {
    val data = collectTemperatures()
    sendToSnapshot(data).subscribe { json, e: Throwable? ->
      if (e != null) {
        logger.error(e) { "Something went wrong" }
        req.response().setStatusCode(500).end()
      } else {
        req.response()
          .putHeader("Content-Type", "application/json")
          .end(json.encode())
      }
    }
  }

  private fun collectTemperatures(): Single<JsonObject> {
    val r1 = fetchTemperature(3000)
    val r2 = fetchTemperature(3001)
    val r3 = fetchTemperature(3002)

    return Single.zip(r1, r2, r3) { j1, j2, j3 ->
      val array = jsonArrayOf(j1.body(), j2.body(), j3.body())
      json { obj("data" to array) }
    }
  }

  private fun fetchTemperature(port: Int): Single<HttpResponse<JsonObject>> =
    webClient.get(port, "localhost", "/")
      .expect(ResponsePredicate.SC_SUCCESS)
      .`as`(BodyCodec.jsonObject())
      .rxSend()

  private fun sendToSnapshot(data: Single<JsonObject>): Single<JsonObject> =
    data.flatMap { json ->
      webClient.post(4000, "localhost", "")
        .expect(ResponsePredicate.SC_SUCCESS)
        .rxSendJsonObject(json)
        .flatMap { Single.just(json) }
    }
}