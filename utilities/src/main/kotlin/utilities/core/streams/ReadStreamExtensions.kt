package utilities.core.streams

import io.vertx.core.streams.ReadStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
fun <T, S : ReadStream<T>> S.handler(
  coroutineScope: CoroutineScope,
  handler: suspend CoroutineScope.(T) -> Unit
): S = handler { t -> coroutineScope.launch { coroutineScope { handler(t) } } } as S

@Suppress("UNCHECKED_CAST")
fun <T, S : ReadStream<T>> S.endHandler(coroutineScope: CoroutineScope, handler: suspend CoroutineScope.() -> Unit): S =
  endHandler { coroutineScope.launch { coroutineScope { handler() } } } as S