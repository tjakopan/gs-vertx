package chapter02.deploy

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking

object Main {
  @JvmStatic
  fun main(args: Array<String>): Unit = runBlocking {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(Deployer()).await()
  }
}