import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import utilities.web.coroutineRespond

@Suppress("unused")
class ActuatorServiceVerticle : CoroutineVerticle() {
  override suspend fun start() {
    vertx.deployVerticle(AdminVerticle::class.java.canonicalName).await()

    val server = vertx.createHttpServer()

    val router = Router.router(vertx)
    router.get("/hello-world")
      .coroutineRespond(this) { ctx -> getGreetingHandler(ctx) }

    server.requestHandler(router)
      .listen(8080)
      .await()
  }
}