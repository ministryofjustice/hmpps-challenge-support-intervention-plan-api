package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.util.Optional

class ReferenceDataIntTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri("/reference-data/outcome-type").exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get().uri("/reference-data/screening-outcome-type").headers(setAuthorisation(roles = listOf())).exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - incorrect role`() {
    webTestClient.get().uri("/reference-data/screening-outcome-type").headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .exchange().expectStatus().isForbidden
  }

  @Test
  fun `404 not found - invalid domain in url`() {
    val response =
      webTestClient.get().uri("/reference-data/wrong-domain").headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI)))
        .exchange().expectStatus().isNotFound.expectBody(ErrorResponse::class.java).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("No resource found failure: Fail to map wrong-domain to Reference Data Type. Reference Data domain name must be one of: area-of-work, contributory-factor-type, decision-outcome-type, role, incident-involvement, incident-location, incident-type, interviewee-role, screening-outcome-type or status")
      assertThat(developerMessage).isEqualTo("Fail to map wrong-domain to Reference Data Type. Reference Data domain name must be one of: area-of-work, contributory-factor-type, decision-outcome-type, role, incident-involvement, incident-location, incident-type, interviewee-role, screening-outcome-type or status")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `get reference excludes inactive codes by default - return active codes only`() {
    val referenceData = webTestClient.getReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, null)
    assertThat(referenceData).isNotEmpty()
    assertThat(referenceData.none { it.deactivatedAt?.isBefore(LocalDateTime.now()) ?: false }).isTrue()
  }

  @Test
  fun `get reference include inactive codes - return both active and inactive codes`() {
    val referenceData = webTestClient.getReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, true)
    val inactiveReferenceData = referenceData.filter { it.deactivatedAt?.isBefore(LocalDateTime.now()) ?: false }
    assertThat(inactiveReferenceData).isNotEmpty()
    assertThat(referenceData).hasSizeGreaterThan(inactiveReferenceData.size)
  }

  private fun WebTestClient.getReferenceData(
    referenceType: ReferenceDataType,
    includeInactive: Boolean?,
  ) = get().uri { builder ->
    builder.path("/reference-data/${referenceType.domain}")
      .queryParamIfPresent("includeInactive", Optional.ofNullable(includeInactive)).build()
  }.headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).exchange().expectStatus().isOk.expectHeader()
    .contentType(MediaType.APPLICATION_JSON).expectBodyList(ReferenceData::class.java).returnResult().responseBody!!
}
