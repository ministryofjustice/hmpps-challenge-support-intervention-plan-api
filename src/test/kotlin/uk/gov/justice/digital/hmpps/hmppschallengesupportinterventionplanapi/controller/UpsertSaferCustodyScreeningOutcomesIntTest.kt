package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpsertSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.util.UUID

class UpsertSaferCustodyScreeningOutcomesIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.put().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.put().uri(urlToTest(UUID.randomUUID()))
      .bodyValue(upsertScreeningOutcomeRequest()).headers(setAuthorisation(roles = listOf()))
      .exchange().expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - incorrect role`() {
    webTestClient.put().uri(urlToTest(UUID.randomUUID()))
      .bodyValue(upsertScreeningOutcomeRequest()).headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .exchange().expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  fun `400 bad request - invalid request values`(
    request: UpsertSaferCustodyScreeningOutcomeRequest,
    userMessage: String,
  ) {
    val response = upsertScreeningOutcomeResponseSpec(UUID.randomUUID(), request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(this.userMessage).isEqualTo(userMessage)
    }
  }

  @Test
  fun `400 bad request - invalid Outcome Type code`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val request = upsertScreeningOutcomeRequest(outcomeTypeCode = "WRONG_CODE")

    val response = upsertScreeningOutcomeResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)

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
    val record = givenCsipRecord(generateCsipRecord())
    val request = upsertScreeningOutcomeRequest()

    val response = upsertScreeningOutcomeResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)

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
    val response = upsertScreeningOutcomeResponseSpec(recordUuid, upsertScreeningOutcomeRequest())
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
  fun `201 created - create new safer custody screening outcome via DPS UI`() {
    val record = dataSetup(generateCsipRecord()) { it.withReferral() }
    val request = upsertScreeningOutcomeRequest(recordedBy = "Safer")

    val response = upsertScreeningOutcome(record.id, request, HttpStatus.CREATED)

    with(response) {
      assertThat(reasonForDecision).isEqualTo(request.reasonForDecision)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(date).isEqualTo(request.date)
      assertThat(recordedBy).isEqualTo(request.recordedBy)
      assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
    }

    val saved = getScreeningOutcome(record.id)
    verifyAudit(saved, RevisionType.ADD, setOf(CsipComponent.SAFER_CUSTODY_SCREENING_OUTCOME))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  @Test
  fun `200 ok - Screening Outcome updated`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withReferral()
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome()
      it
    }

    val request = upsertScreeningOutcomeRequest(
      outcomeTypeCode = "OPE",
      reasonForDecision = "reason for the decision changed",
      date = LocalDate.now().minusDays(7),
      recordedBy = "AN07H3R",
      recordedByDisplayName = "Another Person Display Name",
    )
    val response = upsertScreeningOutcome(record.id, request, HttpStatus.OK)
    with(response) {
      assertThat(reasonForDecision).isEqualTo(request.reasonForDecision)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(date).isEqualTo(request.date)
      assertThat(recordedBy).isEqualTo(request.recordedBy)
      assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
    }

    val saved = getScreeningOutcome(record.id)
    verifyAudit(saved, RevisionType.MOD, setOf(CsipComponent.SAFER_CUSTODY_SCREENING_OUTCOME))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  private fun urlToTest(recordUuid: UUID) = "/csip-records/$recordUuid/referral/safer-custody-screening"

  private fun upsertScreeningOutcomeResponseSpec(
    recordUuid: UUID,
    request: UpsertSaferCustodyScreeningOutcomeRequest,
    username: String = TEST_USER,
  ) = webTestClient.put().uri(urlToTest(recordUuid))
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOf(ROLE_CSIP_UI), isUserToken = true))
    .exchange()

  private fun upsertScreeningOutcome(
    recordUuid: UUID,
    request: UpsertSaferCustodyScreeningOutcomeRequest,
    status: HttpStatus,
    username: String = TEST_USER,
  ) = upsertScreeningOutcomeResponseSpec(recordUuid, request, username)
    .successResponse<SaferCustodyScreeningOutcome>(status)

  private fun getScreeningOutcome(recordUuid: UUID) = csipRecordRepository.getCsipRecord(recordUuid).referral!!.saferCustodyScreeningOutcome!!

  companion object {
    private fun upsertScreeningOutcomeRequest(
      outcomeTypeCode: String = "CUR",
      reasonForDecision: String = "alia",
      recordedBy: String = "recordedBy",
      recordedByDisplayName: String = "${recordedBy}DisplayName",
      date: LocalDate = LocalDate.now(),
    ) = UpsertSaferCustodyScreeningOutcomeRequest(
      outcomeTypeCode,
      date,
      reasonForDecision,
      recordedBy,
      recordedByDisplayName,
    )

    @JvmStatic
    fun invalidRequests() = listOf(
      Arguments.of(
        upsertScreeningOutcomeRequest(outcomeTypeCode = "n".repeat(13)),
        "Validation failure: Screening outcome code must be <= 12 characters",
      ),
      Arguments.of(
        upsertScreeningOutcomeRequest(reasonForDecision = "n".repeat(4001)),
        "Validation failure: Reason for decision must be <= 4000 characters",
      ),
      Arguments.of(
        upsertScreeningOutcomeRequest(recordedBy = "n".repeat(65)),
        "Validation failure: Recorded by username must be <= 64 characters",
      ),
      Arguments.of(
        upsertScreeningOutcomeRequest(recordedByDisplayName = "n".repeat(256)),
        "Validation failure: Recorded by display name must be <= 255 characters",
      ),
    )
  }
}
