import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.1.3"
  kotlin("plugin.spring") version "2.2.21"
  kotlin("plugin.jpa") version "2.2.21"
  id("com.google.cloud.tools.jib") version "3.4.5"
  id("io.gatling.gradle") version "3.14.6.4"
  jacoco
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {

  gatling("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0")
  gatling("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.0")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.7.0")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.24.0")
  implementation("com.fasterxml.uuid:java-uuid-generator:5.1.1")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.11.0")
  implementation("org.hibernate.orm:hibernate-envers")
  implementation("org.springframework.data:spring-data-envers")

  // AWS
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.6.0")

  // Test dependencies
  testImplementation("org.testcontainers:postgresql:1.21.3")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.testcontainers:localstack:1.21.3")
  testImplementation("org.junit.jupiter:junit-jupiter:6.0.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JVM_21
      freeCompilerArgs.add("-Xwhen-guards")
    }
  }

  val copyAgentJar by registering(Copy::class) {
    from("${project.layout.buildDirectory}/libs")
    include("applicationinsights-agent*.jar")
    into("${project.layout.buildDirectory}/agent")
    rename("applicationinsights-agent(.+).jar", "agent.jar")
    dependsOn("assemble")
  }

  val jib by getting {
    dependsOn += copyAgentJar
  }

  val jibBuildTar by getting {
    dependsOn += copyAgentJar
  }

  val jibDockerBuild by getting {
    dependsOn += copyAgentJar
  }

  register("initialiseDatabase", Test::class) {
    include("**/InitialiseDatabase.class")
  }

  test {
    exclude("**/InitialiseDatabase.class")
  }

  getByName("initialiseDatabase") {
    onlyIf { gradle.startParameter.taskNames.contains("initialiseDatabase") }
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

jib {
  container {
    creationTime.set("USE_CURRENT_TIMESTAMP")
    jvmFlags = mutableListOf("-Duser.timezone=Europe/London")
    mainClass = "uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.HmppsChallengeSupportInterventionPlanApiKt"
    user = "2000:2000"
  }
  from {
    image = "eclipse-temurin:21-jre-jammy"
    platforms {
      platform {
        architecture = "amd64"
        os = "linux"
      }
      platform {
        architecture = "arm64"
        os = "linux"
      }
    }
  }
  extraDirectories {
    paths {
      path {
        setFrom(project.layout.buildDirectory)
        includes.add("agent/agent.jar")
      }
      path {
        setFrom(project.rootDir)
        includes.add("applicationinsights*.json")
        into = "/agent"
      }
    }
  }
}

dependencyCheck {
  suppressionFile = ".dependency-check-ignore.xml"
}
