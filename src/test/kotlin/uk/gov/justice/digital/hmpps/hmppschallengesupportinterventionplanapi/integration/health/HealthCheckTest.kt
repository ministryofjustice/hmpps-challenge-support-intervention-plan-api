package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.health

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.ManageUsersExtension.Companion.manageUsers
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckTest : IntegrationTestBase() {

  @Test
  fun `Health page reports ok`() {
    setUpDependencyResponses(HttpStatus.OK)
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
      .jsonPath("components.authApiHealth.status").isEqualTo("UP")
      .jsonPath("components.prisonerSearchApiHealth.status").isEqualTo("UP")
      .jsonPath("components.manageUsersApiHealth.status").isEqualTo("UP")
  }

  @Test
  fun `Health page reports service not available`() {
    setUpDependencyResponses(HttpStatus.SERVICE_UNAVAILABLE)
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isEqualTo(503)
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .jsonPath("components.authApiHealth.status").isEqualTo("DOWN")
      .jsonPath("components.prisonerSearchApiHealth.status").isEqualTo("DOWN")
      .jsonPath("components.manageUsersApiHealth.status").isEqualTo("DOWN")
  }

  @Test
  fun `Health info reports version`() {
    setUpDependencyResponses(HttpStatus.OK)
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
        },
      )
  }

  @Test
  fun `Health ping page is accessible`() {
    setUpDependencyResponses(HttpStatus.OK)
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    setUpDependencyResponses(HttpStatus.OK)
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    setUpDependencyResponses(HttpStatus.OK)
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  fun setUpDependencyResponses(responseStatus: HttpStatus) {
    val dependencies = listOf(hmppsAuth, manageUsers, prisonerSearch)
    dependencies.forEach {
      val url = if (it == hmppsAuth) "/auth/health/ping" else "/health/ping"
      it.stubFor(
        get(url).willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(if (responseStatus == HttpStatus.OK) "{\"status\":\"UP\"}" else "some error")
            .withStatus(responseStatus.value()),
        ),
      )
    }
  }
}
