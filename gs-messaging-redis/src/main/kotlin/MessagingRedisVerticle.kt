import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import redis.ReceiverVerticle
import redis.SenderVerticle

@Suppress("unused")
class MessagingRedisVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val redisConfig = config.getJsonObject("redis")
    vertx.deployVerticle(ReceiverVerticle::class.java.canonicalName, deploymentOptionsOf(config = redisConfig)).await()
    vertx.deployVerticle(SenderVerticle::class.java.canonicalName, deploymentOptionsOf(config = redisConfig)).await()
  }
}