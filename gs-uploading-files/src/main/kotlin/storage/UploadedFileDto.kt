package storage

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.FileUpload

@Suppress("unused")
@DataObject(generateConverter = true, publicConverter = false)
data class UploadedFileDto(var originalFileName: String = "", var uploadedFilePath: String = "", var size: Long = 0) {
  constructor(json: JsonObject) : this() {
    UploadedFileDtoConverter.fromJson(json, this)
  }

  fun toJson(): JsonObject {
    val json = JsonObject()
    UploadedFileDtoConverter.toJson(this, json)
    return json
  }

  fun isEmpty(): Boolean = size == 0L
}

fun FileUpload.toDto() = UploadedFileDto(this.fileName(), this.uploadedFileName(), this.size())
