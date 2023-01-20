package redis

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class Sender(private val connection: StatefulRedisPubSubConnection<String, String>, private val receiver: IReceiver) {
  suspend fun start() {
    while (receiver.count.await() == 0L) {
      logger.info { "Sending message..." }
      connection.async().publish("chat", "Hello from Redis!").await()
      delay(500L)
    }
  }
}