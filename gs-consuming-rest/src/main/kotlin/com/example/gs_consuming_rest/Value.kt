package com.example.gs_consuming_rest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject

@JsonIgnoreProperties(ignoreUnknown = true)
@DataObject(generateConverter = true, publicConverter = false)
data class Value(var id: Long = 0L, var quote: String = "") {
  constructor(json: JsonObject) : this() {
    ValueConverter.fromJson(json, this)
  }

  fun toJson(): JsonObject {
    val json = JsonObject()
    ValueConverter.toJson(this, json)
    return json
  }
}
