package utilities.core

import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun NetServer.connectHandler(
  coroutineScope: CoroutineScope,
  handler: suspend CoroutineScope.(NetSocket) -> Unit
): NetServer = connectHandler { socket -> coroutineScope.launch { coroutineScope { handler(socket) } } }