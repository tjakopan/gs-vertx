package redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.serviceproxy.ServiceBinder
import kotlinx.coroutines.future.await

class ReceiverVerticle : CoroutineVerticle() {
  private var redisClient: RedisClient? = null
  private var connection: StatefulRedisPubSubConnection<String, String>? = null
  private var serviceBinder: ServiceBinder? = null
  private var serviceConsumer: MessageConsumer<JsonObject>? = null

  override suspend fun start() {
    val redisConfig = RedisConfig(config)
    val redisUri = RedisURI.create(redisConfig.uri)
    val redisClient = RedisClient.create()
    val connection = redisClient.connectPubSubAsync(StringCodec.UTF8, redisUri).await()
    val receiver = Receiver.createReceiver(vertx)
    connection.addListener(receiver as Receiver)
    connection.async().subscribe("chat").await()
    val serviceBinder = ServiceBinder(vertx)
    serviceConsumer = serviceBinder.setAddress("receiver")
      .register(IReceiver::class.java, receiver)

    this.redisClient = redisClient
    this.connection = connection
    this.serviceBinder = serviceBinder
  }

  override suspend fun stop() {
    serviceBinder?.unregister(serviceConsumer)
    connection?.closeAsync()?.await()
    redisClient?.close()
  }
}