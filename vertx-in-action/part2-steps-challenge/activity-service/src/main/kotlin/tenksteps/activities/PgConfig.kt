package tenksteps.activities

import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgConnectOptions

internal val pgConnectOptions: PgConnectOptions =
  pgConnectOptionsOf(
    host = "localhost",
    database = "postgres",
    user = "postgres",
    password = "vertx-in-action"
  )