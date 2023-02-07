package chapter04.parsetools

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.parsetools.RecordParser
import io.vertx.kotlin.core.file.openOptionsOf
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

object FetchDatabaseReader {
  @JvmStatic
  fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    val file = vertx.fileSystem().openBlocking("vertx-in-action/chapter04/sample.db", openOptionsOf(read = true))

    val parser = RecordParser.newFixed(4, file)
    parser.pause()
    parser.fetch(1)
    parser.handler { header -> readMagicNumber(header, parser) }
    parser.endHandler { vertx.close() }
  }

  private fun readMagicNumber(header: Buffer, parser: RecordParser) {
    logger.info {
      "Magic number: " +
          "${header.getByte(0)}:${header.getByte(1)}:${header.getByte(2)}:${header.getByte(3)}"
    }
    parser.handler { version -> readVersion(version, parser) }
    parser.fetch(1)
  }

  private fun readVersion(version: Buffer, parser: RecordParser) {
    logger.info { "Version: ${version.getInt(0)}" }
    parser.delimitedMode("\n")
    parser.handler { name -> readName(name, parser) }
    parser.fetch(1)
  }

  private fun readName(name: Buffer, parser: RecordParser) {
    logger.info { "Name: $name" }
    parser.fixedSizeMode(4)
    parser.handler { keyLength -> readKey(keyLength, parser) }
    parser.fetch(1)
  }

  private fun readKey(keyLength: Buffer, parser: RecordParser) {
    parser.fixedSizeMode(keyLength.getInt(0))
    parser.handler { key -> readValue(key.toString(), parser) }
    parser.fetch(1)
  }

  private fun readValue(key: String, parser: RecordParser) {
    parser.fixedSizeMode(4)
    parser.handler { valueLength -> finishEntry(key, valueLength, parser) }
    parser.fetch(1)
  }

  private fun finishEntry(key: String, valueLength: Buffer, parser: RecordParser) {
    parser.fixedSizeMode(valueLength.getInt(0))
    parser.handler { value ->
      logger.info { "Key: $key / Value: $value" }
      parser.fixedSizeMode(4)
      parser.handler { keyLength -> readKey(keyLength, parser) }
      parser.fetch(1)
    }
    parser.fetch(1)
  }
}