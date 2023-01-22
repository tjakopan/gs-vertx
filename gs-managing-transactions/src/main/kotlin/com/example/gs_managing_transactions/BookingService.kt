package com.example.gs_managing_transactions

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.templates.SqlTemplate
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import utilities.serviceproxy.callService
import utilities.serviceproxy.runService
import utilities.sqlclient.withTransaction

private val logger = KotlinLogging.logger { }

@Suppress("SqlResolve")
class BookingService(private val coroutineScope: CoroutineScope, private val vertx: Vertx, private val pool: Pool) :
  IBookingService {
  override fun book(persons: List<String>): Future<Void> = coroutineScope.runService(vertx.dispatcher()) {
    pool.withTransaction(coroutineScope, vertx.dispatcher()) { conn ->
      persons.forEach { person ->
        logger.info { "Booking $person in a seat..." }
        SqlTemplate.forUpdate(conn, "insert into bookings(first_name) values(#{first_name})")
          .execute(mapOf("first_name" to person))
          .await()
      }
    }
  }

  override fun findAllBookings(): Future<List<String>> = coroutineScope.callService(vertx.dispatcher()) {
    pool.query("select first_name from bookings")
      .execute()
      .await()
      .map { r -> r.getString("first_name") }
  }
}