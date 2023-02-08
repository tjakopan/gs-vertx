plugins {
  id("gs.vertx.kotlin-application-conventions")
  kotlin("kapt")
}

dependencies {
  implementation(project(":utilities"))

  compileOnly("io.vertx:vertx-codegen")
  implementation("io.vertx:vertx-service-proxy")

  kapt("io.vertx:vertx-codegen:${vertx.version}:processor")
  kapt("io.vertx:vertx-service-proxy:${vertx.version}")
}

vertx {
  mainVerticleClass = "chapter06.ProxyClient"
}