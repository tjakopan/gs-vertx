plugins {
  id("gs.vertx.kotlin-common-conventions")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-infinispan")
}
