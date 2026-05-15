import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.3.1"
  kotlin("plugin.spring") version "2.3.21"
  kotlin("plugin.jpa") version "2.3.21"
  jacoco
}

ext["jackson-bom.version"] = "3.1.0"
ext["jackson-2-bom.version"] = "2.21.1"

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.2.0")
  implementation("io.sentry:sentry-spring-boot-4:8.41.0")
  implementation("com.fasterxml.uuid:java-uuid-generator:5.2.0")
  implementation("org.springframework.data:spring-data-envers")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

  // AWS
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.3.2")

  // Database dependencies
  runtimeOnly("org.springframework.boot:spring-boot-starter-flyway")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  // Test dependencies
  testImplementation("org.testcontainers:postgresql:1.21.4")
  testImplementation("org.testcontainers:localstack:1.21.4")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.2.0")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
}

kotlin {
  jvmToolchain(25)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JVM_25
      freeCompilerArgs.addAll(
        "-Xwhen-guards",
        "-Xannotation-default-target=param-property",
      )
    }
  }

  test {
    if (project.hasProperty("init-db")) {
      include("**/InitialiseDatabase.class")
    } else {
      exclude("**/InitialiseDatabase.class")
    }
  }
}

tasks.named("test") {
  finalizedBy("jacocoTestReport")
}

tasks.named<JacocoReport>("jacocoTestReport") {
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}
