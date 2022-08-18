package com.example.gs_rest_service

import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.ext.web.client.webClientOptionsOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(VertxExtension::class)
class GetGreetingRouteTest {
  private lateinit var client: WebClient

  @BeforeEach
  fun beforeEach(vertx: Vertx) = runTest {
    vertx.deployVerticle(RestServiceVerticle()).await()
    DatabindCodec.mapper().registerModule(kotlinModule { })
    client = WebClient.create(vertx, webClientOptionsOf(defaultHost = "localhost", defaultPort = 8080))
  }

  @Test
  fun `no param greeting should return default message`(vertx: Vertx) = runTest {
    val req = client.get("/greeting")
      .`as`(BodyCodec.json(Greeting::class.java))

    val res = req.send().await()
    val body = res.body()

    body.content shouldBe "Hello, World!"
  }

  @Test
  fun `param greeting should return tailored message`(vertx: Vertx) = runTest {
    val req = client.request(HttpMethod.GET, "/greeting")
      .addQueryParam("name", "Vert.x community")
      .`as`(BodyCodec.json(Greeting::class.java))

    val res = req.send().await()
    val body = res.body()

    body.content shouldBe "Hello, Vert.x community!"
  }
}
