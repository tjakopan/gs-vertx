package com.example.gs_consuming_rest

import io.vertx.codegen.annotations.ProxyGen
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.CoroutineScope

@ProxyGen
interface IQuoteService {
  fun getRandomQuote(): Future<Quote>
}

const val QUOTE_SERVICE_ADDRESS = "quote-service"

fun createQuoteService(coroutineScope: CoroutineScope, vertx: Vertx, client: WebClient): IQuoteService =
  QuoteService(coroutineScope, vertx, client)

fun createQuoteServiceProxy(vertx: Vertx): IQuoteService = IQuoteServiceVertxEBProxy(vertx, QUOTE_SERVICE_ADDRESS)