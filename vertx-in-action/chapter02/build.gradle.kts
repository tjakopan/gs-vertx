plugins {
  id("gs.vertx.kotlin-application-conventions")
}

dependencies {
  implementation(project(":utilities"))
}

vertx {
  mainVerticleClass = project.properties.getOrDefault("mainClass", "chapter02.hello.HelloVerticle") as String
}
