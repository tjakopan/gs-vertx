package com.example.gs_rest_service

import utilities.web.coroutineRespond
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class RestServiceVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val server = vertx.createHttpServer()

    val router = Router.router(vertx)
    router.get("/greeting")
      .coroutineRespond(this, ::getGreetingHandler)

    server.requestHandler(router)
      .listen(8080)
      .await()
  }
}
