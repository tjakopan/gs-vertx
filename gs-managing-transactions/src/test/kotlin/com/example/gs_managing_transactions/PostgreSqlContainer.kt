package com.example.gs_managing_transactions

import org.testcontainers.containers.PostgreSQLContainer

object PostgreSqlContainer {
  val instance by lazy { startPostgreSqlContainer() }

  private fun startPostgreSqlContainer() = PostgreSQLContainer("postgres:15.1").apply {
    start()
  }
}