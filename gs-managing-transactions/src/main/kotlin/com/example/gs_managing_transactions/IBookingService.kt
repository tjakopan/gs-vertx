package com.example.gs_managing_transactions

import io.vertx.codegen.annotations.ProxyGen
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Pool
import kotlinx.coroutines.CoroutineScope
import utilities.core.replaceWithUnit

@ProxyGen
interface IBookingService {
  fun book(persons: List<String>): Future<Void>
  fun findAllBookings(): Future<List<String>>
}

suspend fun IBookingService.bookSuspending(vararg persons: String): Unit = book(persons.asList()).replaceWithUnit().await()

suspend fun IBookingService.findAllBookingsSuspending(): List<String> = findAllBookings().await()

const val BOOKING_SERVICE_ADDRESS = "booking-service"

fun createBookingService(coroutineScope: CoroutineScope, vertx: Vertx, pool: Pool): IBookingService =
  BookingService(coroutineScope, vertx, pool)

fun createBookingServiceProxy(vertx: Vertx): IBookingService =
  IBookingServiceVertxEBProxy(vertx, BOOKING_SERVICE_ADDRESS)