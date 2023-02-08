plugins {
  id("gs.vertx.kotlin-common-conventions")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-rx-java3")
  implementation("io.vertx:vertx-web-client")
}
