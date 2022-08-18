plugins {
  id("gs.vertx.kotlin-application-conventions")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-web-client")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
}

vertx {
  mainVerticleClass = "com.example.gs_consuming_rest.ConsumingRestVerticle"
}