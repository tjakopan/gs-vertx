plugins {
  id("gs.vertx.kotlin-application-conventions")
  kotlin("kapt")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-pg-client")
  implementation("com.ongres.scram:client:2.1") // Needed for PostgreSQL auth.
  implementation("io.vertx:vertx-sql-client-templates")
  implementation("io.vertx:vertx-service-proxy")

  compileOnly("io.vertx:vertx-codegen")

  kapt("io.vertx:vertx-codegen:${vertx.version}:processor")
  kapt("io.vertx:vertx-service-proxy:${vertx.version}")

  testImplementation("org.testcontainers:postgresql:1.17.6")
}

vertx {
  mainVerticleClass = "com.example.gs_managing_transactions.ManagingTransactionsVerticle"
  config = "config.json"
}
