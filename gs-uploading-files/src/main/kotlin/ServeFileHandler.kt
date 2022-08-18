import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import io.vertx.serviceproxy.ServiceException
import storage.FILE_NOT_FOUND_ERROR
import storage.IStorageService

suspend fun serveFileHandler(storageService: IStorageService, ctx: RoutingContext) {
  val file = ctx.pathParam("file")
  try {
    val path = storageService.load(file).await()
    ctx.response()
      .putHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$file\"")
      .sendFile(path)
      .await()
  } catch (e: ServiceException) {
    if (e.failureCode() == FILE_NOT_FOUND_ERROR) ctx.fail(404)
    else ctx.fail(e)
  } catch (e: Throwable) {
    ctx.fail(e)
  }
}