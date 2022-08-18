import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import mu.KotlinLogging
import storage.StorageVerticle
import storage.createStorageServiceProxy

@Suppress("unused")
class UploadingFilesVerticle : CoroutineVerticle() {
  private val logger = KotlinLogging.logger { }

  override suspend fun start() {
    vertx.deployVerticle(
      StorageVerticle::class.java.canonicalName,
      deploymentOptionsOf(config = config.getJsonObject("storage"))
    ).await()

    val storageService = createStorageServiceProxy(vertx, "storage-service")
    storageService.deleteAll().await()
    storageService.init().await()

    vertx.deployVerticle(HttpServerVerticle::class.java.canonicalName).await()
  }
}