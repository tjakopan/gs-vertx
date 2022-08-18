package storage

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject

@Suppress("unused")
@DataObject(generateConverter = true, publicConverter = false)
data class StorageConfig(var location: String = "upload-dir") {
  constructor(json: JsonObject) : this() {
    StorageConfigConverter.fromJson(json, this)
  }

  fun toJson(): JsonObject {
    val json = JsonObject()
    StorageConfigConverter.toJson(this, json)
    return json
  }
}
