import storage.IStorageService
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import kotlin.io.path.Path
import kotlin.io.path.name

suspend fun listUploadedFilesHandler(storageService: IStorageService, ctx: RoutingContext) {
  try {
    val files = storageService.loadAll().await()
    ctx.put("files", files.map { path -> Path(path).name })
    val session = ctx.session()
    val message = session.get<String>("message")
    session.remove<String>("message")
    if (message != null) ctx.put("message", message)
    ctx.reroute(HttpMethod.GET, "/uploadForm.html")
  } catch (e: Throwable) {
    ctx.fail(e)
  }
}