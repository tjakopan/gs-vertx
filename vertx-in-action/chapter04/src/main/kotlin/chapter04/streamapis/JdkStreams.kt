package chapter04.streamapis

import java.io.File
import java.io.FileInputStream
import java.io.IOException

object JdkStreams {
  @JvmStatic
  fun main(args: Array<String>) {
    val file = File("vertx-in-action/chapter04/build.gradle.kts")
    val buffer = ByteArray(1024)
    try {
      FileInputStream(file).use { `in` ->
        var count = `in`.read(buffer)
        while (count != -1) {
          println(String(buffer, 0, count))
          count = `in`.read(buffer)
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
    println("\n--- DONE")
  }
}