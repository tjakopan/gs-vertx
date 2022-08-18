plugins {
  id("gs.vertx.kotlin-common-conventions")
}

apply<VertxPlugin>()

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")
  implementation("io.vertx:vertx-lang-kotlin")

  testImplementation("io.vertx:vertx-junit5")
}