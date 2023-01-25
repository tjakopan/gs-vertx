package com.example.gs_managing_transactions

import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgConnectOptions
import io.vertx.serviceproxy.ServiceBinder
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import utilities.sqlclient.withTransaction

class ManagingTransactionsVerticle : CoroutineVerticle() {
  private var bookingServiceBinder: ServiceBinder? = null
  private var bookingServiceConsumer: MessageConsumer<JsonObject>? = null

  override suspend fun start() {
    val dbOptions = config.getJsonObject("db")
    val connectOptions = PgConnectOptions(dbOptions.getJsonObject("connection"))
    val poolOptions = PoolOptions(dbOptions.getJsonObject("pool"))
    val pool = Pool.pool(vertx, connectOptions, poolOptions)
    pool.withTransaction(this) { conn ->
      conn.query("drop table if exists bookings").execute().await()
      conn.query("create table bookings(id serial, first_name varchar(5) not null)").execute().await()
    }

    val bookingService = createBookingService(this, pool)
    bookingServiceBinder = ServiceBinder(vertx).setAddress(BOOKING_SERVICE_ADDRESS)
    bookingServiceConsumer = bookingServiceBinder!!.register(IBookingService::class.java, bookingService)
  }

  override suspend fun stop() {
    bookingServiceBinder?.unregister(bookingServiceConsumer)
  }
}