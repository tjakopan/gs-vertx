package tenksteps.publicapi

import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.predicate.ResponsePredicate
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.ext.auth.jwt.jwtAuthOptionsOf
import io.vertx.kotlin.ext.auth.jwtOptionsOf
import io.vertx.kotlin.ext.auth.pubSecKeyOptionsOf
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import utilities.web.handler
import kotlin.time.Duration.Companion.days

private const val HTTP_PORT = 4000
private val logger = KotlinLogging.logger { }

class PublicApiVerticle : CoroutineVerticle() {
  private lateinit var webClient: WebClient
  private lateinit var jwtAuth: JWTAuth

  override suspend fun start() {
    val publicKey = publicKey()
    val privateKey = privateKey()

    jwtAuth = JWTAuth.create(
      vertx,
      jwtAuthOptionsOf(
        pubSecKeys = listOf(
          pubSecKeyOptionsOf(algorithm = "RS256", buffer = publicKey),
          pubSecKeyOptionsOf(algorithm = "RS256", buffer = privateKey)
        )
      )
    )

    val router = Router.router(vertx)

    val allowedHeaders = setOf(
      "x-requested-with",
      "Access-Control-Allow-Origin",
      "origin",
      "Content-Type",
      "accept",
      "Authorization"
    )

    val allowedMethods = setOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS, HttpMethod.PUT)

    router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods))

    val bodyHandler = BodyHandler.create()
    router.post().handler(bodyHandler)
    router.put().handler(bodyHandler)

    val prefix = "/api/v1"
    val jwtHandler = JWTAuthHandler.create(jwtAuth)

    // Account
    router.post("$prefix/register").handler(this) { ctx -> register(ctx) }
    router.post("$prefix/token").handler(this) { ctx -> token(ctx) }

    // Profile
    router.get("$prefix/:username").handler(jwtHandler).handler(::checkUser)
      .handler(this) { ctx -> fetchUser(ctx) }
    router.put("$prefix/:username").handler(jwtHandler).handler(::checkUser)
      .handler(this) { ctx -> updateUser(ctx) }

    // Data
    router.get("$prefix/:username/total").handler(jwtHandler).handler(::checkUser)
      .handler(this) { ctx -> totalSteps(ctx) }
    router.get("$prefix/:username/:year/:month").handler(jwtHandler).handler(::checkUser)
      .handler(this) { ctx -> monthlySteps(ctx) }
    router.get("$prefix/:username/:year/:month/:day").handler(jwtHandler).handler(::checkUser)
      .handler(this) { ctx -> dailySteps(ctx) }

    webClient = WebClient.create(vertx)

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(HTTP_PORT)
      .await()
  }

  private suspend fun register(ctx: RoutingContext) {
    try {
      val res = webClient.post(3000, "localhost", "/register")
        .putHeader("Content-Type", "application/json")
        .sendJson(ctx.body().asJsonObject())
        .await()
      sendStatusCode(ctx, res.statusCode())
    } catch (e: Throwable) {
      sendBadGateway(ctx, e)
    }
  }

  private suspend fun token(ctx: RoutingContext) {
    try {
      val payload = ctx.body().asJsonObject()
      val username = payload.getString("username")
      webClient.post(3000, "localhost", "/authenticate")
        .expect(ResponsePredicate.SC_SUCCESS)
        .sendJson(payload)
        .await()
      val deviceId = fetchUserDetails(username).getString("deviceId")
      val token = makeJwtToken(username, deviceId)
      sendToken(ctx, token)
    } catch (e: Throwable) {
      handleAuthError(ctx, e)
    }
  }

  private suspend fun fetchUserDetails(username: String): JsonObject =
    webClient.get(3000, "localhost", "/$username")
      .expect(ResponsePredicate.SC_OK)
      .`as`(BodyCodec.jsonObject())
      .send()
      .await()
      .body()

  private fun makeJwtToken(username: String, deviceId: String): String {
    val claims = jsonObjectOf("deviceId" to deviceId)
    val jwtOptions = jwtOptionsOf(
      algorithm = "RS256",
      expiresInMinutes = 7.days.inWholeMinutes.toInt(),
      issuer = "10k-steps-api",
      subject = username
    )
    return jwtAuth.generateToken(claims, jwtOptions)
  }

  private fun sendToken(ctx: RoutingContext, token: String) {
    ctx.response().putHeader("Content-Type", "application/jwt").end(token)
  }

  private fun checkUser(ctx: RoutingContext) {
    val subject = ctx.user().principal().getString("sub")
    if (ctx.pathParam("username") != subject) sendStatusCode(ctx, 403)
    else ctx.next()
  }

  private suspend fun fetchUser(ctx: RoutingContext) {
    try {
      val res = webClient.get(3000, "localhost", "/${ctx.pathParam("username")}")
        .`as`(BodyCodec.jsonObject())
        .send()
        .await()
      forwardJsonOrStatusCode(ctx, res)
    } catch (e: Throwable) {
      sendBadGateway(ctx, e)
    }
  }

  private suspend fun updateUser(ctx: RoutingContext) {
    try {
      webClient.put(3000, "localhost", "/${ctx.pathParam("username")}")
        .putHeader("Content-Type", "application/json")
        .expect(ResponsePredicate.SC_OK)
        .sendBuffer(ctx.body().buffer())
        .await()
      ctx.response().end()
    } catch (e: Throwable) {
      sendBadGateway(ctx, e)
    }
  }

  private suspend fun totalSteps(ctx: RoutingContext) {
    val deviceId = ctx.user().principal().getString("deviceId")
    try {
      val res = webClient.get(3001, "localhost", "/$deviceId/total")
        .`as`(BodyCodec.jsonObject())
        .send()
        .await()
      forwardJsonOrStatusCode(ctx, res)
    } catch (e: Throwable) {
      sendBadGateway(ctx, e)
    }
  }

  private suspend fun monthlySteps(ctx: RoutingContext) {
    val deviceId = ctx.user().principal().getString("deviceId")
    val year = ctx.pathParam("year")
    val month = ctx.pathParam("month")
    try {
      val res = webClient.get(3001, "localhost", "/$deviceId/$year/$month")
        .`as`(BodyCodec.jsonObject())
        .send()
        .await()
      forwardJsonOrStatusCode(ctx, res)
    } catch (e: Throwable) {
      sendBadGateway(ctx, e)
    }
  }

  private suspend fun dailySteps(ctx: RoutingContext) {
    val deviceId = ctx.user().principal().getString("deviceId")
    val year = ctx.pathParam("year")
    val month = ctx.pathParam("month")
    val day = ctx.pathParam("day")
    try {
      val res = webClient.get(3001, "localhost", "/$deviceId/$year/$month/$day")
        .`as`(BodyCodec.jsonObject())
        .send()
        .await()
      forwardJsonOrStatusCode(ctx, res)
    } catch (e: Throwable) {
      sendBadGateway(ctx, e)
    }
  }

  private fun forwardJsonOrStatusCode(ctx: RoutingContext, res: HttpResponse<JsonObject>) {
    if (res.statusCode() != 200) sendStatusCode(ctx, res.statusCode())
    else ctx.response().putHeader("Content-Type", "application/json").end(res.body().encode())
  }

  private fun handleAuthError(ctx: RoutingContext, e: Throwable) {
    logger.error(e) { "Authentication error" }
    ctx.fail(401)
  }

  private fun sendStatusCode(ctx: RoutingContext, code: Int) {
    ctx.response().setStatusCode(code).end()
  }

  private fun sendBadGateway(ctx: RoutingContext, e: Throwable) {
    logger.error(e) { "Woops" }
    ctx.fail(502)
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      val vertx = Vertx.vertx()
      try {
        vertx.deployVerticle(PublicApiVerticle()).await()
        logger.info { "HTTP server started on port $HTTP_PORT" }
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
      }
    }
  }
}