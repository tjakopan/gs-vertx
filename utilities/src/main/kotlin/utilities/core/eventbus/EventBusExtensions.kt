package utilities.core.eventbus

import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun <T> EventBus.consumer(
  coroutineScope: CoroutineScope,
  address: String,
  handler: suspend CoroutineScope.(Message<T>) -> Unit
): MessageConsumer<T> = consumer(address) { msg -> coroutineScope.launch { coroutineScope { handler(msg) } } }