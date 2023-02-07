package chapter03

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import mu.KotlinLogging
import java.text.DecimalFormat

private val logger = KotlinLogging.logger { }

class Listener : CoroutineVerticle() {
  private val format: DecimalFormat = DecimalFormat("#.##")

  override suspend fun start() {
    val bus = vertx.eventBus()
    bus.consumer<JsonObject>("sensor.updates") { msg ->
      val body = msg.body()
      val id = body.getString("id")
      val temperature = format.format(body.getDouble("temp"))
      logger.info { "$id reports a temperature ~${temperature}C" }
    }
  }
}