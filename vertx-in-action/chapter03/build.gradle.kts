plugins {
  id("gs.vertx.kotlin-application-conventions")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-infinispan")
}

vertx {
  mainVerticleClass = project.properties.getOrDefault("mainClass", "chapter03.local.Main") as String
}
