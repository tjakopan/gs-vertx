package chapter02.future

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking

class SomeVerticle : CoroutineVerticle() {
  override suspend fun start() {
    vertx.createHttpServer()
      .requestHandler { req -> req.response().end("Ok") }
      .listen(8080)
      .await()
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      val vertx = Vertx.vertx()
      vertx.deployVerticle(SomeVerticle()).await()
    }
  }
}