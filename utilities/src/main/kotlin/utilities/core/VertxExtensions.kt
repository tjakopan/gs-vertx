package utilities.core

import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration

fun Vertx.setTimer(coroutineScope: CoroutineScope, delay: Duration, handler: suspend CoroutineScope.(Long) -> Unit): Long =
  setTimer(delay.inWholeMilliseconds) { id -> coroutineScope.launch { coroutineScope { handler(id) } } }

fun Vertx.setPeriodic(
  coroutineScope: CoroutineScope,
  delay: Duration,
  handler: suspend CoroutineScope.(Long) -> Unit
): Long = setPeriodic(delay.inWholeMilliseconds) { id -> coroutineScope.launch { coroutineScope { handler(id) } } }