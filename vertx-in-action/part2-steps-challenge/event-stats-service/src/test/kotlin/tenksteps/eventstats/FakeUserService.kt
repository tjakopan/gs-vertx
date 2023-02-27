package tenksteps.eventstats

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class FakeUserService : CoroutineVerticle() {
  private lateinit var deviceId: String

  override suspend fun start() {
    val router = Router.router(vertx)
    router.get("/owns/:deviceId").handler(::owns)
    router.get("/:username").handler(::username)
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(3000)
      .await()
  }

  private fun owns(ctx: RoutingContext) {
    logger.info { "Device ownership request ${ctx.request().path()}" }
    deviceId = ctx.pathParam("deviceId")
    val notAllData = jsonObjectOf("username" to "Foo", "deviceId" to deviceId)
    ctx.response().putHeader("Content-Type", "application/json").end(notAllData.encode())
  }

  private fun username(ctx: RoutingContext) {
    logger.info { "User data request ${ctx.request().path()}" }
    val notAllData = jsonObjectOf(
      "username" to "Foo",
      "email" to "foo@mail.tld",
      "deviceId" to deviceId,
      "city" to "Lyon",
      "makePublic" to true
    )
    ctx.response().putHeader("Content-Type", "application/json").end(notAllData.encode())
  }
}