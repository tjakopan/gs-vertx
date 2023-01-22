package com.example.gs_managing_transactions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.await
import io.vertx.serviceproxy.ServiceException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mu.KotlinLogging
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.BeforeTest
import kotlin.test.Test

private val logger = KotlinLogging.logger { }

@ExperimentalCoroutinesApi
@ExtendWith(VertxExtension::class)
class BookingServiceIT {
  private val postgres = PostgreSqlContainer.instance
  private lateinit var service: IBookingService

  @BeforeTest
  fun setUp(vertx: Vertx) = runTest {
    val config = json {
      obj(
        "db" to obj(
          "connection" to obj(
            "port" to postgres.getMappedPort(5432),
            "host" to postgres.host,
            "database" to postgres.databaseName,
            "user" to postgres.username,
            "password" to postgres.password
          ),
          "pool" to obj(
            "max-size" to 5
          )
        )
      )
    }
    vertx.deployVerticle(ManagingTransactionsVerticle(), deploymentOptionsOf(config = config)).await()
    service = createBookingServiceProxy(vertx)
  }

  @Test
  fun test() = runTest {
    service.bookSuspending("Alice", "Bob", "Carol")
    service.findAllBookingsSuspending() shouldHaveSize 3
    logger.info { "Alice, Bob and Carol have been booked." }

    val action = suspend { service.bookSuspending("Chris", "Samuel") }
    val e = shouldThrow<ServiceException> { action() }
      .message shouldContain "value too long for type character varying(5)"
    logger.info { "The following exception is expected because 'Samuel' is too big for the DB." }
    logger.error(e)

    service.findAllBookingsSuspending().forEach { person ->
      logger.info { "So far, $person is booked." }
    }
    logger.info { "You shouldn't see Chris or Samuel. Samuel violated DB constraints, and Chris was rolled back in the same transaction." }
    service.findAllBookingsSuspending() shouldHaveSize 3
  }
}