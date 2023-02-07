package chapter02.blocker

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking

class BlockEventLoop : CoroutineVerticle() {
  override suspend fun start() {
    vertx.setTimer(1000) {
      while (true) {
      }
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      val vertx = Vertx.vertx()
      vertx.deployVerticle(BlockEventLoop()).await()
    }
  }
}