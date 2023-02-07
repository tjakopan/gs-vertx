package chapter04.parsetools

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.core.file.openOptionsOf

object SampleDatabaseWriter {
  @JvmStatic
  fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    val file = vertx.fileSystem().openBlocking(
      "vertx-in-action/chapter04/sample.db",
      openOptionsOf(write = true, create = true)
    )

    val buffer = Buffer.buffer()

    // Magic number
    buffer.appendBytes(byteArrayOf(1, 2, 3, 4))

    // Version
    buffer.appendInt(2)

    // DB name
    buffer.appendString("Sample database\n")

    // Entry 1
    var key = "abc"
    var value = "123456-abcdef"
    buffer.appendInt(key.length)
      .appendString(key)
      .appendInt(value.length)
      .appendString(value)

    // Entry 2
    key = "foo@bar"
    value = "Foo Bar Baz"
    buffer.appendInt(key.length)
      .appendString(key)
      .appendInt(value.length)
      .appendString(value)

    file.end(buffer) { vertx.close() }
  }
}