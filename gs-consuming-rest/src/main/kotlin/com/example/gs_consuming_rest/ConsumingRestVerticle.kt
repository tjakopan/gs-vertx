package com.example.gs_consuming_rest

import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

@Suppress("unused")
class ConsumingRestVerticle : CoroutineVerticle() {
  override suspend fun start() {
    DatabindCodec.mapper().registerModule(kotlinModule())
    DatabindCodec.prettyMapper().registerModule(kotlinModule())
    val client = WebClient.create(vertx)
    val quote =
      client.getAbs("https://gistcdn.githack.com/ayan-b/ff0441b5a8d6c489b58659ffb849aff4/raw/e1c5ca10f7bea57edd793c4189ea8339de534b45/response.json")
        .`as`(BodyCodec.json(Quote::class.java))
        .send()
        .await()
        .body()
    logger.info { quote }
  }
}