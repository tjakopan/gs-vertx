package redis

import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.shareddata.Counter
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import utilities.serviceproxy.runServiceWithResult

class Receiver private constructor(private val scope: CoroutineScope, private val vertx: Vertx) :
  RedisPubSubAdapter<String, String>(), IReceiver {
  private val logger = KotlinLogging.logger { }
  private lateinit var counter: Counter
  override val count: Future<Long>
    get() = scope.runServiceWithResult(vertx.dispatcher()) { counter.get().await() }

  private suspend fun init() {
    val sharedData = vertx.sharedData()
    counter = sharedData.getLocalCounter("receiver-counter").await()
  }

  override fun message(channel: String, message: String) {
    logger.info { "Received <$message>" }
    counter.incrementAndGet()
  }

  companion object {
    suspend fun create(scope: CoroutineScope, vertx: Vertx): IReceiver {
      val receiver = Receiver(scope, vertx)
      receiver.init()
      return receiver
    }
  }
}

fun createReceiverProxy(vertx: Vertx, address: String): IReceiver = IReceiverVertxEBProxy(vertx, address)