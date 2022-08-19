package redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.future.await

class SenderVerticle : CoroutineVerticle() {
  private var redisClient: RedisClient? = null
  private var connection: StatefulRedisPubSubConnection<String, String>? = null

  override suspend fun start() {
    val redisConfig = RedisConfig(config)
    val redisUri = RedisURI.create(redisConfig.uri)
    val redisClient = RedisClient.create()
    val connection = redisClient.connectPubSubAsync(StringCodec.UTF8, redisUri).await()
    val receiver = createReceiverProxy(vertx, "receiver")
    val sender = Sender(connection, receiver)
    sender.start()
    this.redisClient = redisClient
    this.connection = connection
  }

  override suspend fun stop() {
    connection?.closeAsync()?.await()
    redisClient?.close()
  }
}