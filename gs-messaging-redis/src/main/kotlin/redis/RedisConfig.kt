package redis

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject

@DataObject(generateConverter = true, publicConverter = false)
data class RedisConfig(val uri: String = "redis://127.0.0.1:6379") {
  constructor(json: JsonObject) : this() {
    RedisConfigConverter.fromJson(json, this)
  }

  fun toJson(): JsonObject {
    val json = JsonObject()
    RedisConfigConverter.toJson(this, json)
    return json
  }
}
