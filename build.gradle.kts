import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.0"
  kotlin("plugin.spring") version "2.0.0"
  kotlin("plugin.jpa") version "2.0.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.0.0")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  implementation("io.hypersistence:hypersistence-utils-hibernate-62:3.7.5")

  // AWS
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:4.0.0")

  // Test dependencies
  testImplementation("org.testcontainers:postgresql:1.19.8")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
  testImplementation("org.wiremock:wiremock-standalone:3.5.4")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.1")
  testImplementation("org.testcontainers:localstack:1.19.8")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
  }
}
