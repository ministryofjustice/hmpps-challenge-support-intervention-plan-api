package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class ReferenceDataIntTest : IntegrationTestBase() {
  @Test
  fun `400 bad request - invalid domain`() {
    val response = webTestClient.get()
      .uri("/reference-data/wrong-domain")
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage)
        .isEqualTo("Validation failure: Fail to map wrong-domain to Reference Data Type. Reference Data domain name must be one of: area-of-work, contributory-factor-type, role, incident-involvement, incident-location, incident-type, interviewee-role, or outcome-type")
      assertThat(developerMessage)
        .isEqualTo("Fail to map wrong-domain to Reference Data Type. Reference Data domain name must be one of: area-of-work, contributory-factor-type, role, incident-involvement, incident-location, incident-type, interviewee-role, or outcome-type")
      assertThat(moreInfo).isNull()
    }
  }
}
