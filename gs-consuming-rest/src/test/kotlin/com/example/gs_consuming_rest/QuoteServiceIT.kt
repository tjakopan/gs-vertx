package com.example.gs_consuming_rest

import io.kotest.matchers.nulls.shouldNotBeNull
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(VertxExtension::class)
class QuoteServiceIT {
  private lateinit var service: IQuoteService

  @BeforeTest
  fun setUp(vertx: Vertx) = runTest {
    vertx.deployVerticle(ConsumingRestVerticle()).await()
    service = createQuoteServiceProxy(vertx)
  }

  @Test
  fun test() = runTest {
    val quote = service.getRandomQuote().await()

    quote.shouldNotBeNull()
  }
}