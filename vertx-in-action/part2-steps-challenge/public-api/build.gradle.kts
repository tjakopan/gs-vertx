plugins {
  id("gs.vertx.kotlin-application-conventions")
  kotlin("kapt")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-web-client")
  implementation("io.vertx:vertx-auth-jwt")

  testImplementation("io.rest-assured:rest-assured:5.3.0")
  testImplementation("io.rest-assured:kotlin-extensions:5.3.0")
  testImplementation(project(":vertx-in-action:part2-steps-challenge:user-profile-service"))
  testImplementation(project(":vertx-in-action:part2-steps-challenge:activity-service"))
  testImplementation("io.vertx:vertx-pg-client")
  testImplementation("org.testcontainers:junit-jupiter:1.17.6")
}

vertx {
  mainVerticleClass = "tenksteps.publicapi.PublicApiVerticle"
}