package tenksteps.activities

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import mu.KotlinLogging
import tenksteps.activities.SqlQueries.dailyStepsCount
import tenksteps.activities.SqlQueries.monthlyStepsCount
import tenksteps.activities.SqlQueries.rankingLast24Hours
import tenksteps.activities.SqlQueries.totalStepsCount
import utilities.web.handler
import java.time.DateTimeException
import java.time.LocalDateTime

internal const val HTTP_PORT = 3001
private val logger = KotlinLogging.logger { }

class ActivityApiVerticle : CoroutineVerticle() {
  private lateinit var pgPool: PgPool

  override suspend fun start() {
    pgPool = PgPool.pool(vertx, pgConnectOptions, poolOptionsOf())

    val router = Router.router(vertx)
    router.get("/:deviceId/total").handler(this) { ctx -> totalSteps(ctx) }
    router.get("/:deviceId/:year/:month").handler(this) { ctx -> stepsOnMonth(ctx) }
    router.get("/:deviceId/:year/:month/:day").handler(this) { ctx -> stepsOnDay(ctx) }
    router.get("/ranking-last-24-hours").handler(this) { ctx -> ranking(ctx) }

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(HTTP_PORT)
      .await()
  }

  private suspend fun totalSteps(ctx: RoutingContext) {
    val deviceId = ctx.pathParam("deviceId")
    val params = Tuple.of(deviceId)
    try {
      val row = pgPool.preparedQuery(totalStepsCount)
        .execute(params)
        .await()
        .first()
      sendCount(ctx, row)
    } catch (e: Throwable) {
      handleError(ctx, e)
    }
  }

  private suspend fun stepsOnMonth(ctx: RoutingContext) {
    try {
      val deviceId = ctx.pathParam("deviceId")
      val dateTime = LocalDateTime.of(
        ctx.pathParam("year").toInt(),
        ctx.pathParam("month").toInt(),
        1,
        0,
        0
      )
      val params = Tuple.of(deviceId, dateTime)
      val row = pgPool.preparedQuery(monthlyStepsCount)
        .execute(params)
        .await()
        .first()
      sendCount(ctx, row)
    } catch (e: Throwable) {
      when (e) {
        is DateTimeException -> sendBadRequest(ctx)
        is NumberFormatException -> sendBadRequest(ctx)
        else -> handleError(ctx, e)
      }
    }
  }

  private suspend fun stepsOnDay(ctx: RoutingContext) {
    try {
      val deviceId = ctx.pathParam("deviceId")
      val dateTime = LocalDateTime.of(
        ctx.pathParam("year").toInt(),
        ctx.pathParam("month").toInt(),
        ctx.pathParam("day").toInt(),
        0,
        0
      )
      val params = Tuple.of(deviceId, dateTime)
      val row = pgPool.preparedQuery(dailyStepsCount)
        .execute(params)
        .await()
        .first()
      sendCount(ctx, row)
    } catch (e: Throwable) {
      when (e) {
        is DateTimeException -> sendBadRequest(ctx)
        is NumberFormatException -> sendBadRequest(ctx)
        else -> handleError(ctx, e)
      }
    }
  }

  private suspend fun ranking(ctx: RoutingContext) {
    try {
      val rows = pgPool.preparedQuery(rankingLast24Hours)
        .execute()
        .await()
      sendRanking(ctx, rows)
    } catch (e: Throwable) {
      handleError(ctx, e)
    }
  }

  private fun sendRanking(ctx: RoutingContext, rows: RowSet<Row>) {
    val jsonRows = rows.map { row ->
      jsonObjectOf("deviceId" to row.getValue("device_id"), "stepsCount" to row.getValue("steps"))
    }
    val data = jsonArrayOf(*jsonRows.toTypedArray())
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(data.encode())
  }

  private fun sendBadRequest(ctx: RoutingContext) {
    ctx.response().setStatusCode(400).end()
  }

  private fun sendCount(ctx: RoutingContext, row: Row) {
    val count = row.getInteger(0)
    if (count != null) {
      val payload = jsonObjectOf("count" to count)
      ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(payload.encode())
    } else send404(ctx)
  }

  private fun send404(ctx: RoutingContext) {
    ctx.response().setStatusCode(404).end()
  }

  private fun handleError(ctx: RoutingContext, e: Throwable) {
    logger.error(e) { "Woops" }
    ctx.response().setStatusCode(500).end()
  }
}