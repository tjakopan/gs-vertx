import utilities.web.coroutineHandler
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.handler.TemplateHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import storage.createStorageServiceProxy

class HttpServerVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val server = vertx.createHttpServer()

    val storageService = createStorageServiceProxy(vertx, "storage-service")

    val templateEngine = ThymeleafTemplateEngine.create(vertx)
    val templateHandler = TemplateHandler.create(templateEngine)
    val router = Router.router(vertx)
    val sessionStore = LocalSessionStore.create(vertx)
    val sessionHandler = SessionHandler.create(sessionStore)
    router.route().handler(sessionHandler)
    router.get("/").coroutineHandler(this) { ctx -> listUploadedFilesHandler(storageService, ctx) }
    router.getWithRegex(".+\\.html").handler(templateHandler)
    router.get("/files/:file").coroutineHandler(this) { ctx -> serveFileHandler(storageService, ctx) }
    router.post("/")
      .handler(BodyHandler.create())
      .coroutineHandler(this) { ctx -> fileUploadHandler(storageService, ctx) }

    server.requestHandler(router)
      .listen(8080)
      .await()
  }
}