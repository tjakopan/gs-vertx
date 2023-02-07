package utilities.core.http

import io.vertx.core.http.HttpServerResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun HttpServerResponse.endHandler(
  coroutineScope: CoroutineScope,
  handler: suspend CoroutineScope.() -> Unit
): HttpServerResponse = endHandler { coroutineScope.launch { coroutineScope { handler() } } }