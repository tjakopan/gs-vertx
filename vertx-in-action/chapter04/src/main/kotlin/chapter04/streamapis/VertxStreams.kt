package chapter04.streamapis

import io.vertx.core.Vertx
import io.vertx.kotlin.core.file.openOptionsOf
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking

object VertxStreams {
  @JvmStatic
  fun main(args: Array<String>): Unit = runBlocking {
    val vertx = Vertx.vertx()
    val opts = openOptionsOf(read = true)
    try {
      val file = vertx.fileSystem().open("vertx-in-action/chapter04/build.gradle.kts", opts).await()
      file.handler(::println)
        .exceptionHandler(Throwable::printStackTrace)
        .endHandler {
          println("\n--- DONE")
          vertx.close()
        }
    } catch (e: Throwable) {
      e.printStackTrace()
    }
  }
}