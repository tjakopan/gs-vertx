package com.example.gs_consuming_rest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject

@Suppress("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
@DataObject(generateConverter = true, publicConverter = false)
data class Quote(var type: String = "", var value: Value = Value()) {
  constructor(json: JsonObject) : this() {
    QuoteConverter.fromJson(json, this)
  }

  fun toJson(): JsonObject {
    val json = JsonObject()
    QuoteConverter.toJson(this, json)
    return json
  }
}
