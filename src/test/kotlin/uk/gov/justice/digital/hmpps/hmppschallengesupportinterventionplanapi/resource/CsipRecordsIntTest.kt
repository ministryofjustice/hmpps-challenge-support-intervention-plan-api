package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CsipRecordsIntTest : IntegrationTestBase() {
  @Test
  fun `403 forbidden - no required role`() {
    val response = webTestClient.get()
      .uri("/prisoners/AB123456/csip-records")
      .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .exchange()
      .expectStatus().isForbidden
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage)
        .isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage)
        .isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }
}
