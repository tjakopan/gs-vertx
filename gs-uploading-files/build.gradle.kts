plugins {
  id("gs.vertx.kotlin-application-conventions")
  kotlin("kapt")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-web-templ-thymeleaf")
  implementation("io.vertx:vertx-service-proxy")

  compileOnly("io.vertx:vertx-codegen")

  kapt("io.vertx:vertx-codegen:${vertx.version}:processor")
  kapt("io.vertx:vertx-service-proxy:${vertx.version}")

  testImplementation("io.vertx:vertx-web-client")
}

vertx {
  mainVerticleClass = "UploadingFilesVerticle"
}
