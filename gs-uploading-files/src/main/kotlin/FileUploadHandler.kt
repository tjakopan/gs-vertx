import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import storage.IStorageService
import storage.toDto

suspend fun fileUploadHandler(storageService: IStorageService, ctx: RoutingContext) {
  val uploads = ctx.fileUploads()
  if (uploads.isNotEmpty()) {
    try {
      uploads.forEach { f -> storageService.store(f.toDto()).await() }
      val fileNames = uploads.joinToString { f -> f.fileName() }
      val session = ctx.session()
      session.put("message", "You successfully uploaded $fileNames!")
      ctx.redirect("/")
    } catch (e: Throwable) {
      ctx.fail(e)
    }
  }
}