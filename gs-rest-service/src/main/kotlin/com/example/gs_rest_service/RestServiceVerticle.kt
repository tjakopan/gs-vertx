package com.example.gs_rest_service

import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import utilities.web.respond

class RestServiceVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val server = vertx.createHttpServer()

    val router = Router.router(vertx)
    router.get("/greeting")
      .respond(this) { ctx -> getGreetingHandler(ctx) }

    server.requestHandler(router)
      .listen(8080)
      .await()
  }
}
