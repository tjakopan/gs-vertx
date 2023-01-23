plugins {
  id("gs.vertx.kotlin-application-conventions")
  kotlin("kapt")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-web-client")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.1")
  implementation("io.vertx:vertx-service-proxy")

  compileOnly("io.vertx:vertx-codegen")

  kapt("io.vertx:vertx-codegen:${vertx.version}:processor")
  kapt("io.vertx:vertx-service-proxy:${vertx.version}")
}

vertx {
  mainVerticleClass = "com.example.gs_consuming_rest.ConsumingRestVerticle"
}