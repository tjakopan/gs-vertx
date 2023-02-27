plugins {
  id("gs.vertx.kotlin-application-conventions")
  kotlin("kapt")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-mongo-client")
  implementation("io.vertx:vertx-auth-mongo")

  testImplementation("io.rest-assured:rest-assured:5.3.0")
  testImplementation("io.rest-assured:kotlin-extensions:5.3.0")
  testImplementation("org.testcontainers:junit-jupiter:1.17.6")
}

vertx {
  mainVerticleClass = "tenksteps.userprofiles.UserProfileApiVerticle"
}