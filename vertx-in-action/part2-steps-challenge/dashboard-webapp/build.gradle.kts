import com.github.gradle.node.npm.task.NpmTask

plugins {
  id("com.github.node-gradle.node") version "3.5.1"
  id("gs.vertx.kotlin-application-conventions")
  kotlin("kapt")
}

dependencies {
  implementation(project(":utilities"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-web-client")
  implementation("io.vertx:vertx-kafka-client")
}

vertx {
  mainVerticleClass = "tenksteps.webapp.dashboard.DashboardWebAppVerticle"
}

tasks.register<NpmTask>("buildVueApp") {
  dependsOn("npmInstall")
  args.set(listOf("run", "build"))
}

tasks.register<Copy>("copyVueDist") {
  dependsOn("buildVueApp")
  from("$projectDir/dist")
  into("$projectDir/src/main/resources/webroot/assets")
}

val processResources by tasks.named("processResources") {
  dependsOn("copyVueDist")
}

val clean by tasks.named<Delete>("clean") {
  delete("$projectDir/dist")
  delete("$projectDir/src/main/resources/webroot/assets")
}
