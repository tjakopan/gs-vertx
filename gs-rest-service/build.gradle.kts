plugins {
  id("gs.vertx.kotlin-application-conventions")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-web")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")

  testImplementation("io.vertx:vertx-web-client")
}

vertx {
  mainVerticleClass = "com.example.gs_rest_service.RestServiceVerticle"
}
