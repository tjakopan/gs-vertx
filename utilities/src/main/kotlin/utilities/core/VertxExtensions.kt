package utilities.core

import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun Vertx.setTimer(coroutineScope: CoroutineScope, delay: Long, handler: suspend CoroutineScope.(Long) -> Unit): Long =
  setTimer(delay) { id -> coroutineScope.launch { coroutineScope { handler(id) } } }

fun Vertx.setPeriodic(
  coroutineScope: CoroutineScope,
  delay: Long,
  handler: suspend CoroutineScope.(Long) -> Unit
): Long = setPeriodic(delay) { id -> coroutineScope.launch { coroutineScope { handler(id) } } }