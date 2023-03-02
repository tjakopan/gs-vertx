plugins {
  id("gs.vertx.kotlin-application-conventions")
  kotlin("kapt")
}

dependencies {
  implementation(project(":utilities"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
  implementation("io.lettuce:lettuce-core:6.2.3.RELEASE")
  implementation("io.vertx:vertx-service-proxy")

  compileOnly("io.vertx:vertx-codegen")

  kapt("io.vertx:vertx-codegen:${vertx.version}:processor")
  kapt("io.vertx:vertx-service-proxy:${vertx.version}")
}

vertx {
  mainVerticleClass = "MessagingRedisVerticle"
}
