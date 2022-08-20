import io.vertx.core.Future
import io.vertx.ext.dropwizard.MetricsService
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class AdminVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val server = vertx.createHttpServer()

    val router = Router.router(vertx)
    router.get("/health").handler(HealthCheckHandler.create(vertx))

    val metricsService = MetricsService.create(vertx)
    router.get("/metrics").respond { Future.succeededFuture(metricsService.getMetricsSnapshot("")) }

    server.requestHandler(router)
      .listen(8081)
      .await()
  }
}