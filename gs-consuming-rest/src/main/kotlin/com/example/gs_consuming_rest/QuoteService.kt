package com.example.gs_consuming_rest

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import utilities.serviceproxy.callService

private val logger = KotlinLogging.logger { }

class QuoteService(
  private val coroutineScope: CoroutineScope,
  private val vertx: Vertx,
  private val client: WebClient
) : IQuoteService {
  override fun getRandomQuote(): Future<Quote> = coroutineScope.callService(vertx.dispatcher()) {
    val quote =
      client.getAbs("https://gistcdn.githack.com/ayan-b/ff0441b5a8d6c489b58659ffb849aff4/raw/e1c5ca10f7bea57edd793c4189ea8339de534b45/response.json")
        .`as`(BodyCodec.json(Quote::class.java))
        .send()
        .await()
        .body()
    logger.info { quote }
    quote
  }
}