plugins {
  id("gs.vertx.kotlin-library-conventions")
}

dependencies {
//  implementation(kotlin("reflect"))
  compileOnly("io.vertx:vertx-web")
  compileOnly("io.vertx:vertx-sql-client")
}
