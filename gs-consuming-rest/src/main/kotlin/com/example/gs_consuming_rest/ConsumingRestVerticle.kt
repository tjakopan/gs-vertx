package com.example.gs_consuming_rest

import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.serviceproxy.ServiceBinder

@Suppress("unused")
class ConsumingRestVerticle : CoroutineVerticle() {
  private var quoteServiceBinder: ServiceBinder? = null
  private var quoteServiceConsumer: MessageConsumer<JsonObject>? = null

  override suspend fun start() {
    DatabindCodec.mapper().registerModule(kotlinModule())
    DatabindCodec.prettyMapper().registerModule(kotlinModule())
    val client = WebClient.create(vertx)
    val quoteService = createQuoteService(this, vertx, client)
    quoteServiceBinder = ServiceBinder(vertx).setAddress(QUOTE_SERVICE_ADDRESS)
    quoteServiceConsumer = quoteServiceBinder!!.register(IQuoteService::class.java, quoteService)
  }

  override suspend fun stop() {
    quoteServiceBinder?.unregister(quoteServiceConsumer)
  }
}