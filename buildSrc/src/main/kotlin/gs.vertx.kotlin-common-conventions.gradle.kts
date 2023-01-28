import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

  implementation(platform("io.vertx:vertx-stack-depchain:4.3.7"))
  implementation("io.vertx:vertx-core")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")
  implementation("io.vertx:vertx-lang-kotlin")

  implementation("ch.qos.logback:logback-classic:1.4.5")
  implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")

  testImplementation(kotlin("test-junit5"))
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("io.kotest:kotest-assertions-core:5.5.4")
}

java.sourceCompatibility = JavaVersion.VERSION_18

tasks.withType<KotlinCompile>() {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = JavaVersion.VERSION_18.toString()
  }
}

tasks.named<Test>("test") {
  useJUnitPlatform() {
    systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
  }
}
