import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.7.10"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

  implementation(platform("io.vertx:vertx-stack-depchain:4.3.3"))
  implementation("io.vertx:vertx-core")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")
  implementation("io.vertx:vertx-lang-kotlin")

  implementation("ch.qos.logback:logback-classic:1.2.11")
  implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")

  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("io.kotest:kotest-assertions-core:5.4.1")
}

java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<KotlinCompile>() {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = JavaVersion.VERSION_17.toString()
  }
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
