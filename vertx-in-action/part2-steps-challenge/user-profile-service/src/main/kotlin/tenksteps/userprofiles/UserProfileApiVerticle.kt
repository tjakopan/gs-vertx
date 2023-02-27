package tenksteps.userprofiles

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.auth.mongo.MongoAuthentication
import io.vertx.ext.auth.mongo.MongoUserUtil
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.ext.auth.mongo.mongoAuthenticationOptionsOf
import io.vertx.kotlin.ext.auth.mongo.mongoAuthorizationOptionsOf
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import utilities.web.handler

private const val HTTP_PORT = 3000
private val logger = KotlinLogging.logger { }

class UserProfileApiVerticle : CoroutineVerticle() {
  private lateinit var mongoClient: MongoClient
  private lateinit var authProvider: MongoAuthentication
  private lateinit var userUtil: MongoUserUtil

  private val mongoConfig: JsonObject = jsonObjectOf("host" to "localhost", "port" to 27017, "db_name" to "profiles")

  override suspend fun start() {
    mongoClient = MongoClient.createShared(vertx, mongoConfig)

    authProvider = MongoAuthentication.create(mongoClient, mongoAuthenticationOptionsOf())
    userUtil = MongoUserUtil.create(mongoClient, mongoAuthenticationOptionsOf(), mongoAuthorizationOptionsOf())

    val router = Router.router(vertx)
    val bodyHandler = BodyHandler.create()
    router.post().handler(bodyHandler)
    router.put().handler(bodyHandler)
    router.post("/register").handler(::validateRegistration).handler(this) { ctx -> register(ctx) }
    router.get("/:username").handler(this) { ctx -> fetchUser(ctx) }
    router.put("/:username").handler(this) { ctx -> updateUser(ctx) }
    router.post("/authenticate").handler(this) { ctx -> authenticate(ctx) }
    router.get("/owns/:deviceId").handler(this) { ctx -> whoOwns(ctx) }

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(HTTP_PORT)
      .await()
  }

  private fun validateRegistration(ctx: RoutingContext) {
    val body = jsonBody(ctx)
    if (anyRegistrationFieldIsMissing(body) || anyRegistrationFieldIsWrong(body)) ctx.fail(400)
    else ctx.next()
  }

  private fun anyRegistrationFieldIsMissing(body: JsonObject): Boolean =
    !(body.containsKey("username") && body.containsKey("password") && body.containsKey("email") &&
        body.containsKey("city") && body.containsKey("deviceId") && body.containsKey("makePublic"))

  private val validUsername: Regex = "\\w[\\w+|-]*".toRegex()
  private val validDeviceId: Regex = "\\w[\\w+|-]*".toRegex()

  // Email regexp from https://www.owasp.org/index.php/OWASP_Validation_Regex_Repository
  private val validEmail: Regex =
    "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$".toRegex()

  private fun anyRegistrationFieldIsWrong(body: JsonObject): Boolean =
    !validUsername.matches(body.getString("username")) || !validEmail.matches(body.getString("email")) ||
        body.getString("password").trim().isEmpty() || !validDeviceId.matches(body.getString("deviceId"))

  private suspend fun register(ctx: RoutingContext) {
    val body = jsonBody(ctx)
    val username = body.getString("username")
    val password = body.getString("password")

    val extraInfo = jsonObjectOf(
      "\$set" to jsonObjectOf(
        "email" to body.getString("email"),
        "city" to body.getString("city"),
        "deviceId" to body.getString("deviceId"),
        "makePublic" to body.getBoolean("makePublic")
      )
    )

    try {
      val docId = userUtil.createUser(username, password).await()
      insertExtraInfo(extraInfo, docId)
      completeRegistration(ctx)
    } catch (e: Throwable) {
      handleRegistrationError(ctx, e)
    }
  }

  private suspend fun insertExtraInfo(extraInfo: JsonObject, docId: String) {
    val query = jsonObjectOf("_id" to docId)
    try {
      mongoClient.findOneAndUpdate("user", query, extraInfo).await()
    } catch (e: Throwable) {
      deleteIncompleteUser(query, e)
    }
  }

  private suspend fun deleteIncompleteUser(query: JsonObject, e: Throwable) {
    try {
      if (isIndexViolated(e)) mongoClient.removeDocument("user", query).await()
    } finally {
      throw e
    }
  }

  private fun completeRegistration(ctx: RoutingContext) {
    ctx.response().end()
  }

  private fun handleRegistrationError(ctx: RoutingContext, e: Throwable) {
    if (isIndexViolated(e)) {
      logger.error(e) { "Registration failure: ${e.message}" }
      ctx.fail(409)
    } else {
      fail500(ctx, e)
    }
  }

  private suspend fun fetchUser(ctx: RoutingContext) {
    val username = ctx.pathParam("username")

    val query = jsonObjectOf("username" to username)

    val fields =
      jsonObjectOf("_id" to 0, "username" to 1, "email" to 1, "deviceId" to 1, "city" to 1, "makePublic" to 1)

    try {
      val json = mongoClient.findOne("user", query, fields).await()
      if (json != null) completeFetchRequest(ctx, json)
      else ctx.fail(404)
    } catch (e: Throwable) {
      handleFetchError(ctx, e)
    }
  }

  private fun completeFetchRequest(ctx: RoutingContext, json: JsonObject) {
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(json.encode())
  }

  private fun handleFetchError(ctx: RoutingContext, e: Throwable) {
    if (e is NoSuchElementException) ctx.fail(404)
    else fail500(ctx, e)
  }

  private suspend fun updateUser(ctx: RoutingContext) {
    val username = ctx.pathParam("username")
    val body = jsonBody(ctx)

    val query = jsonObjectOf("username" to username)
    var updates = jsonObjectOf()
    if (body.containsKey("city")) updates.put("city", body.getString("city"))
    if (body.containsKey("email")) updates.put("email", body.getString("email"))
    if (body.containsKey("makePublic")) updates.put("makePublic", body.getBoolean("makePublic"))

    if (updates.isEmpty) {
      ctx.response()
        .setStatusCode(200)
        .end()
      return
    }
    updates = jsonObjectOf("\$set" to updates)

    try {
      mongoClient.findOneAndUpdate("user", query, updates).await()
      completeEmptySuccess(ctx)
    } catch (e: Throwable) {
      handleUpdateError(ctx, e)
    }
  }

  private fun handleUpdateError(ctx: RoutingContext, e: Throwable) {
    fail500(ctx, e)
  }

  private suspend fun authenticate(ctx: RoutingContext) {
    try {
      authProvider.authenticate(UsernamePasswordCredentials(jsonBody(ctx))).await()
      completeEmptySuccess(ctx)
    } catch (e: Throwable) {
      handleAuthenticationError(ctx, e)
    }
  }

  private suspend fun whoOwns(ctx: RoutingContext) {
    val deviceId = ctx.pathParam("deviceId")

    val query = jsonObjectOf("deviceId" to deviceId)

    val fields = jsonObjectOf("_id" to 0, "username" to 1, "deviceId" to 1)

    try {
      val json = mongoClient.findOne("user", query, fields).await()
      if (json != null) completeFetchRequest(ctx, json) else ctx.fail(404)
    } catch (e: Throwable) {
      handleFetchError(ctx, e)
    }
  }

  private fun handleAuthenticationError(ctx: RoutingContext, e: Throwable) {
    logger.error(e) { "Authentication problem ${e.message}" }
    ctx.response().setStatusCode(401).end()
  }

  private fun completeEmptySuccess(ctx: RoutingContext) {
    ctx.response().setStatusCode(200).end()
  }

  private fun fail500(ctx: RoutingContext, e: Throwable) {
    logger.error(e) { "Woops" }
    ctx.fail(500)
  }

  private fun isIndexViolated(e: Throwable): Boolean = e.message?.contains("E11000") ?: false

  private fun jsonBody(ctx: RoutingContext): JsonObject =
    if (ctx.body().length() == 0) jsonObjectOf()
    else ctx.body().asJsonObject()

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
      try {
        Vertx.vertx().deployVerticle(UserProfileApiVerticle()).await()
        logger.info { "HTTP server started on port $HTTP_PORT" }
      } catch (e: Throwable) {
        logger.error(e) { "Woops" }
      }
    }
  }
}