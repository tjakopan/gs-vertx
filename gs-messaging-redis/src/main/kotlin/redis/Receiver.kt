package redis

import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.shareddata.Counter
import io.vertx.kotlin.coroutines.await
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class Receiver private constructor(private val vertx: Vertx) :
  RedisPubSubAdapter<String, String>(), IReceiver {
  private lateinit var counter: Counter
  override val count: Future<Long>
    get() = counter.get()

  private suspend fun init() {
    val sharedData = vertx.sharedData()
    counter = sharedData.getLocalCounter("receiver-counter").await()
  }

  override fun message(channel: String, message: String) {
    logger.info { "Received <$message>" }
    counter.incrementAndGet()
  }

  companion object {
    suspend fun createReceiver(vertx: Vertx): IReceiver {
      val receiver = Receiver(vertx)
      receiver.init()
      return receiver
    }
  }
}

fun createReceiverProxy(vertx: Vertx, address: String): IReceiver = IReceiverVertxEBProxy(vertx, address)