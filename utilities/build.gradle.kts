plugins {
  id("gs.vertx.kotlin-library-conventions")
}

dependencies {
  compileOnly("io.vertx:vertx-web")
  compileOnly("io.vertx:vertx-sql-client")
}
