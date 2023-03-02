package chapter04.jukebox

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.file.AsyncFile
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.openOptionsOf
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import mu.KotlinLogging
import utilities.core.eventbus.consumer
import utilities.core.http.requestHandler
import utilities.core.setPeriodic
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger { }

private const val TRACKS_PATH = "vertx-in-action/chapter04/tracks"

class Jukebox : CoroutineVerticle() {
  override suspend fun start() {
    logger.info { "Start" }

    val eventBus = vertx.eventBus()
    eventBus.consumer<Any>(this, "jukebox.list") { msg -> list(msg) }
    eventBus.consumer("jukebox.schedule", ::schedule)
    eventBus.consumer<Any>("jukebox.play") { play() }
    eventBus.consumer<Any>("jukebox.pause") { pause() }

    vertx.createHttpServer()
      .requestHandler(this) { req -> httpHandler(req) }
      .listen(8080)
      .await()

    vertx.setPeriodic(this, 100.milliseconds) { streamAudioChunk() }
  }

  private enum class State { PLAYING, PAUSED }

  private var currentMode: State = State.PAUSED
  private val playlist: ArrayDeque<String> = ArrayDeque()

  private suspend fun list(request: Message<*>) {
    try {
      val files = vertx.fileSystem().readDir(TRACKS_PATH, ".*mp3$").await()
        .map(::File)
        .map(File::getName)
      val json = json { obj("files" to array(files)) }
      request.reply(json)
    } catch (e: Throwable) {
      logger.error(e) { "readDir failed" }
      request.fail(500, e.message)
    }
  }

  private fun play() {
    logger.info { "Play" }
    currentMode = State.PLAYING
  }

  private fun pause() {
    logger.info { "Pause" }
    currentMode = State.PAUSED
  }

  private fun schedule(request: Message<JsonObject>) {
    val file = request.body().getString("file")
    logger.info { "Scheduling $file" }
    if (playlist.isEmpty() && currentMode == State.PAUSED) currentMode = State.PLAYING
    playlist.addLast(file)
  }

  private suspend fun httpHandler(request: HttpServerRequest) {
    logger.info { "${request.method()} '${request.path()}' ${request.remoteAddress()}" }
    if ("/" == request.path()) {
      openAudioStream(request)
      return
    }
    if (request.path().startsWith("/download/")) {
      val sanitizedPath = request.path().substring(10).replace("/", "")
      download(sanitizedPath, request)
      return
    }
    request.response().setStatusCode(404).end()
  }

  private val streamers: MutableSet<HttpServerResponse> = mutableSetOf()

  private fun openAudioStream(request: HttpServerRequest) {
    logger.info { "New streamer" }
    val response = request.response()
      .putHeader("Content-Type", "audio/mpeg")
      .setChunked(true)
    streamers.add(response)
    response.endHandler {
      streamers.remove(response)
      logger.info { "A streamer left" }
    }
  }

  private suspend fun download(path: String, request: HttpServerRequest) {
    val file = "$TRACKS_PATH/$path"
    if (!vertx.fileSystem().exists(file).await()) {
      request.response().setStatusCode(404).end()
      return
    }
    val opts = openOptionsOf(read = true)
    try {
      val asyncFile = vertx.fileSystem().open(file, opts).await()
      downloadFile(asyncFile, request)
    } catch (e: Throwable) {
      logger.error(e) { "Read failed" }
      request.response().setStatusCode(500).end()
    }
  }

  private fun downloadFile(file: AsyncFile, request: HttpServerRequest) {
    val response = request.response().apply {
      statusCode = 200
      putHeader("Content-Type", "audio/mpeg")
      isChunked = true
    }

    file.handler { buffer ->
      response.write(buffer)
      if (response.writeQueueFull()) {
        file.pause()
        response.drainHandler { file.resume() }
      }
    }

    file.endHandler { response.end() }
  }

  private fun downloadFilePipe(file: AsyncFile, request: HttpServerRequest) {
    val response = request.response().apply {
      statusCode = 200
      putHeader("Content-Type", "audio/mpeg")
      isChunked = true
    }
    file.pipeTo(response)
  }

  private var currentFile: AsyncFile? = null
  private var positionInFile: Long = 0

  private suspend fun streamAudioChunk() {
    if (currentMode == State.PAUSED) return
    if (currentFile == null && playlist.isEmpty()) {
      currentMode = State.PAUSED
      return
    }
    if (currentFile == null) openNextFile()
    try {
      val buffer = currentFile!!.read(Buffer.buffer(4096), 0, positionInFile, 4096).await()
      processReadBuffer(buffer)
    } catch (e: Throwable) {
      logger.error(e) { "Read failed" }
      closeCurrentFile()
    }
  }

  private suspend fun openNextFile() {
    logger.info { "Opening ${playlist.firstOrNull()}" }
    val opts = openOptionsOf(read = true)
    currentFile = vertx.fileSystem().open("$TRACKS_PATH/${playlist.removeFirstOrNull()}", opts).await()
    positionInFile = 0
  }

  private fun closeCurrentFile() {
    logger.info { "Closing file" }
    positionInFile = 0
    currentFile?.close()
    currentFile = null
  }

  private fun processReadBuffer(buffer: Buffer) {
    logger.info { "Read ${buffer.length()} bytes from pos $positionInFile" }
    positionInFile += buffer.length()
    if (buffer.length() == 0) {
      closeCurrentFile()
      return
    }
    streamers.forEach { streamer ->
      if (!streamer.writeQueueFull()) streamer.write(buffer.copy())
    }
  }
}