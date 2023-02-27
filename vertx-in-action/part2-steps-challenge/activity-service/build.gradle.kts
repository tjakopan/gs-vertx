plugins {
  id("gs.vertx.kotlin-application-conventions")
  kotlin("kapt")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-kafka-client")
  implementation("io.vertx:vertx-pg-client")
  runtimeOnly("com.ongres.scram:client:2.1") // Needed for PostgreSQL auth.

  testImplementation("io.rest-assured:rest-assured:5.3.0")
  testImplementation("io.rest-assured:kotlin-extensions:5.3.0")
  testImplementation("org.testcontainers:junit-jupiter:1.17.6")
}

vertx {
  mainVerticleClass = "tenksteps.activities.Main"
}