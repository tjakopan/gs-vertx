plugins {
  id("gs.vertx.kotlin-application-conventions")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-health-check")
  implementation("io.vertx:vertx-dropwizard-metrics")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.1")

  testImplementation("io.vertx:vertx-web-client")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.2")
  // system-stubs uses older byte buddy which does not work with java 19.
  testImplementation("net.bytebuddy:byte-buddy-agent:1.12.22")
  testImplementation("net.bytebuddy:byte-buddy:1.12.22")
}

vertx {
  mainVerticleClass = "ActuatorServiceVerticle"
  jvmArgs = listOf("-Dvertx.metrics.options.enabled=true")
}
