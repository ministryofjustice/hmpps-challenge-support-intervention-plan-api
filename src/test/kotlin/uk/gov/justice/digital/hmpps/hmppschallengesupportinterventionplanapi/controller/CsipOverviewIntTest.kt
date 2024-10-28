package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipCounts
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipOverview

class CsipOverviewIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(urlToTest("OVE")).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no required role`() {
    val response = getCsipOverviewResponseSpec("OVE", "WRONG_ROLE").errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `200 ok - when no csip record exists for prison`() {
    val response = getCsipOverview("NON_EXISTENT")
    assertThat(response.counts).isEqualTo(CsipCounts.NONE)
  }

  @Test
  fun `200 ok - correctly counts csip records for overview`() {
    val response = getCsipOverview()
  }

  private fun getCsipOverviewResponseSpec(
    prisonCode: String = "OVE",
    role: String = ROLE_CSIP_UI,
  ): WebTestClient.ResponseSpec =
    webTestClient.get().uri(urlToTest(prisonCode))
      .headers(setAuthorisation(roles = listOfNotNull(role)))
      .exchange()

  private fun getCsipOverview(prisonCode: String = "OVE", role: String = ROLE_CSIP_UI): CsipOverview =
    getCsipOverviewResponseSpec(prisonCode, role).successResponse()

  private fun urlToTest(prisonCode: String) = "/prisons/$prisonCode/csip-records/overview"
}
