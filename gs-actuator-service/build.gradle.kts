plugins {
  id("gs.vertx.kotlin-application-conventions")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-health-check")
  implementation("io.vertx:vertx-dropwizard-metrics")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")

  testImplementation("io.vertx:vertx-web-client")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.1")
}

vertx {
  mainVerticleClass = "ActuatorServiceVerticle"
  jvmArgs = listOf("-Dvertx.metrics.options.enabled=true")
}
