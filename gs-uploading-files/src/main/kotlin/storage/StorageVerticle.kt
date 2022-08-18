package storage

import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.serviceproxy.ServiceBinder

class StorageVerticle : CoroutineVerticle() {
  private var storageServiceBinder: ServiceBinder? = null
  private var storageServiceConsumer: MessageConsumer<JsonObject>? = null

  override suspend fun start() {
    val storageConfig = StorageConfig(config)
    val storageService = createStorageService(this, vertx, storageConfig)
    storageServiceBinder = ServiceBinder(vertx)
    storageServiceConsumer = storageServiceBinder?.setAddress("storage-service")
      ?.register(IStorageService::class.java, storageService)
  }

  override suspend fun stop() {
    storageServiceBinder?.unregister(storageServiceConsumer)
  }
}