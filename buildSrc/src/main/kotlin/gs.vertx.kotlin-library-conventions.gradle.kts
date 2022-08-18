import gradle.kotlin.dsl.accessors._657ac3b2e5072282e18494eb2b5fc9d6.implementation
import gradle.kotlin.dsl.accessors._657ac3b2e5072282e18494eb2b5fc9d6.testImplementation

plugins {
  id("gs.vertx.kotlin-common-conventions")
  `java-library`
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("io.vertx:vertx-core:4.3.3")
  implementation("io.vertx:vertx-lang-kotlin-coroutines:4.3.3")
  implementation("io.vertx:vertx-lang-kotlin:4.3.3")

  testImplementation("io.vertx:vertx-junit5:4.3.3")
}