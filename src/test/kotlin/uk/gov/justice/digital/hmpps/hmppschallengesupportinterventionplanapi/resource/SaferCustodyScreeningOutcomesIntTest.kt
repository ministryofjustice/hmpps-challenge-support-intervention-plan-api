package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.util.UUID

class SaferCustodyScreeningOutcomesIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post().uri(urlToTest(UUID.randomUUID()))
      .bodyValue(createScreeningOutcomeRequest()).headers(setAuthorisation(roles = listOf()))
      .exchange().expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - incorrect role`() {
    webTestClient.post().uri(urlToTest(UUID.randomUUID()))
      .bodyValue(createScreeningOutcomeRequest()).headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .exchange().expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  fun `400 bad request - invalid request values`(
    request: CreateSaferCustodyScreeningOutcomeRequest,
    userMessage: String,
  ) {
    val response = createScreeningOutcomeResponseSpec(UUID.randomUUID(), request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(this.userMessage).isEqualTo(userMessage)
    }
  }

  @Test
  fun `400 bad request - invalid Outcome Type code`() {
    val prisonNumber = givenValidPrisonNumber("S1234MF")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber).withReferral())
    val request = createScreeningOutcomeRequest(outcomeTypeCode = "WRONG_CODE")

    val response = createScreeningOutcomeResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: SCREENING_OUTCOME_TYPE is invalid")
      assertThat(developerMessage).isEqualTo("Details => SCREENING_OUTCOME_TYPE:WRONG_CODE")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - CSIP record missing a referral`() {
    val prisonNumber = givenValidPrisonNumber("S1234MF")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber))
    val request = createScreeningOutcomeRequest()

    val response = createScreeningOutcomeResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record is missing a referral.")
      assertThat(developerMessage).isEqualTo("CSIP Record is missing a referral.")
      assertThat(moreInfo).isEqualTo(record.id.toString())
    }
  }

  @Test
  fun `404 not found - CSIP record not found`() {
    val recordUuid = UUID.randomUUID()
    val response = createScreeningOutcomeResponseSpec(recordUuid, createScreeningOutcomeRequest())
      .expectStatus().isNotFound
      .expectBody<ErrorResponse>()
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: CSIP Record not found")
      assertThat(developerMessage).isEqualTo("CSIP Record not found with identifier $recordUuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `409 conflict - CSIP record already has Screening Outcome created`() {
    val prisonNumber = givenValidPrisonNumber("S1234AE")
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withReferral()
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome()
      it
    }

    val request = createScreeningOutcomeRequest()
    val response = createScreeningOutcomeResponseSpec(record.id, request).errorResponse(HttpStatus.CONFLICT)

    with(response) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Conflict failure: Referral already has a Safer Custody Screening Outcome")
      assertThat(developerMessage).isEqualTo("Referral already has a Safer Custody Screening Outcome")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `create safer custody screening outcome via DPS UI`() {
    val prisonNumber = givenValidPrisonNumber("S1234CD")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral() }
    val request = createScreeningOutcomeRequest(recordedBy = "Safer")

    val response = createScreeningOutcome(record.id, request)

    with(response) {
      assertThat(reasonForDecision).isEqualTo(request.reasonForDecision)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(date).isEqualTo(request.date)
      assertThat(recordedBy).isEqualTo(request.recordedBy)
      assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
    }

    val saved = getScreeningOutcome(record.id)
    verifyAudit(saved, RevisionType.ADD, setOf(CsipComponent.SAFER_CUSTODY_SCREENING_OUTCOME))
    verifyDomainEvents(prisonNumber, record.id, CSIP_UPDATED)
  }

  private fun urlToTest(recordUuid: UUID) = "/csip-records/$recordUuid/referral/safer-custody-screening"

  private fun createScreeningOutcomeResponseSpec(
    recordUuid: UUID,
    request: CreateSaferCustodyScreeningOutcomeRequest,
    username: String = TEST_USER,
  ) = webTestClient.post().uri(urlToTest(recordUuid))
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOf(ROLE_CSIP_UI), isUserToken = true))
    .exchange()

  private fun createScreeningOutcome(
    recordUuid: UUID,
    request: CreateSaferCustodyScreeningOutcomeRequest,
    username: String = TEST_USER,
  ) = createScreeningOutcomeResponseSpec(recordUuid, request, username)
    .successResponse<SaferCustodyScreeningOutcome>(HttpStatus.CREATED)

  private fun getScreeningOutcome(recordUuid: UUID) =
    csipRecordRepository.getCsipRecord(recordUuid).referral!!.saferCustodyScreeningOutcome!!

  companion object {
    private fun createScreeningOutcomeRequest(
      outcomeTypeCode: String = "CUR",
      reasonForDecision: String = "alia",
      recordedBy: String = "recordedBy",
      recordedByDisplayName: String = "${recordedBy}DisplayName",
    ) = CreateSaferCustodyScreeningOutcomeRequest(
      outcomeTypeCode,
      LocalDate.now(),
      reasonForDecision,
      recordedBy,
      recordedByDisplayName,
    )

    @JvmStatic
    fun invalidRequests() = listOf(
      Arguments.of(
        createScreeningOutcomeRequest(outcomeTypeCode = "n".repeat(13)),
        "Validation failure: Outcome Type code must be <= 12 characters",
      ),
      Arguments.of(
        createScreeningOutcomeRequest(reasonForDecision = "n".repeat(4001)),
        "Validation failure: Reason for Decision must be <= 4000 characters",
      ),
      Arguments.of(
        createScreeningOutcomeRequest(recordedBy = "n".repeat(65)),
        "Validation failure: Recorded by username must be <= 64 characters",
      ),
      Arguments.of(
        createScreeningOutcomeRequest(recordedByDisplayName = "n".repeat(256)),
        "Validation failure: Recorded by display name must be <= 255 characters",
      ),
    )
  }
}
