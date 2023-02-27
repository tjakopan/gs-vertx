package tenksteps.webapp.users

import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private const val HTTP_PORT = 8080
private val logger = KotlinLogging.logger { }

class UserWebAppVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val router = Router.router(vertx)
    router.route().handler(StaticHandler.create("webroot/assets"))
    router.get("/*").handler { ctx -> ctx.reroute("/index.html") }
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(HTTP_PORT)
      .await()
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      try {
        Vertx.vertx().deployVerticle(UserWebAppVerticle()).await()
        logger.info { "HTTP server started on port $HTTP_PORT" }
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
      }
    }
  }
}