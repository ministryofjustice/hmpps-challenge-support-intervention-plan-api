package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.INVESTIGATION_REQUIRED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.CaseNotesServer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.OFFENDER_IDENTIFIER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest

class CaseNotesControllerIntTest : IntegrationTestBase() {

  @BeforeEach
  fun resetCaseNotesServer() {
    caseNotesServer.resetRequests()
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest()).bodyValue(validRequest()).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = createRequestSpec(validRequest(), role = role).errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `202 accepted - initiates downstream case notes retrieval with no response body`() {
    caseNotesServer.stubGetCaseNotes(OFFENDER_IDENTIFIER)

    createRequestSpec(validRequest())
      .expectStatus().isAccepted
      .expectBody().isEmpty

    caseNotesServer.verify(
      exactly(1),
      postRequestedFor(urlEqualTo("/search/case-notes/$OFFENDER_IDENTIFIER"))
        .withRequestBody(matchingJsonPath("$.includeSensitive", equalTo("false")))
        .withRequestBody(matchingJsonPath("$.page", equalTo("1")))
        .withRequestBody(matchingJsonPath("$.size", equalTo("100")))
        .withRequestBody(matchingJsonPath("$.sort", equalTo("occurredAt,desc")))
        .withRequestBody(matchingJsonPath("$.occurredFrom"))
        .withRequestBody(matchingJsonPath("$.occurredTo")),
    )
  }

  @Test
  fun `202 accepted - does not call case notes when outcome does not require investigation`() {
    createRequestSpec(validRequest(outcomeTypeCode = "ACC"))
      .expectStatus().isAccepted
      .expectBody().isEmpty

    caseNotesServer.verify(
      exactly(0),
      postRequestedFor(urlEqualTo("/search/case-notes/$OFFENDER_IDENTIFIER")),
    )
  }

  private fun createRequestSpec(
    request: CaseNotesLookupRequest,
    role: String? = ROLE_CSIP_UI,
    username: String = TEST_USER,
  ) = webTestClient.post()
    .uri(urlToTest())
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role)))
    .exchange()

  private fun validRequest(
    offenderIdentifier: String = OFFENDER_IDENTIFIER,
    outcomeTypeCode: String = INVESTIGATION_REQUIRED,
    includeSensitive: Boolean = false,
    caseload: String = "LEI",
  ) = CaseNotesLookupRequest(
    offenderIdentifier = offenderIdentifier,
    includeSensitive = includeSensitive,
    outcomeTypeCode = outcomeTypeCode,
    caseload = caseload,
  )

  private fun urlToTest() = "/initiate-case-notes-retrieval?period=90&pageSize=100"

  companion object {
    @JvmField
    internal val caseNotesServer = CaseNotesServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      caseNotesServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      caseNotesServer.stop()
    }
  }
}
