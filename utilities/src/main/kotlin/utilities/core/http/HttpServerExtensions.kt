package utilities.core.http

import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun HttpServer.requestHandler(
  coroutineScope: CoroutineScope,
  handler: suspend CoroutineScope.(HttpServerRequest) -> Unit
): HttpServer = requestHandler { req -> coroutineScope.launch { coroutineScope { handler(req) } } }