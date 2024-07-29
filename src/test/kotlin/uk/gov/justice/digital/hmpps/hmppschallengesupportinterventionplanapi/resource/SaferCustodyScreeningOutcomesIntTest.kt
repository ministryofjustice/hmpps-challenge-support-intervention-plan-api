package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class SaferCustodyScreeningOutcomesIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/safer-custody-screening").exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/safer-custody-screening")
      .bodyValue(createScreeningOutcomeRequest()).headers(setAuthorisation()).headers(setCsipRequestContext())
      .exchange().expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - incorrect role`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/safer-custody-screening")
      .bodyValue(createScreeningOutcomeRequest()).headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .headers(setCsipRequestContext()).exchange().expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/safer-custody-screening")
      .bodyValue(createScreeningOutcomeRequest()).headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .headers { it.set(SOURCE, "INVALID") }.exchange().expectStatus().isBadRequest
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
    val csipRecord = givenCsipRecord(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = createScreeningOutcomeRequest(outcomeTypeCode = "WRONG_CODE")

    val response = createScreeningOutcomeResponseSpec(recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: OUTCOME_TYPE is invalid")
      assertThat(developerMessage).isEqualTo("Details => OUTCOME_TYPE:WRONG_CODE")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - CSIP record missing a referral`() {
    val prisonNumber = givenValidPrisonNumber("S1234MF")
    val csipRecord = givenCsipRecord(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = createScreeningOutcomeRequest()

    val response = createScreeningOutcomeResponseSpec(recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record with UUID: $recordUuid is missing a referral.")
      assertThat(developerMessage).isEqualTo("CSIP Record with UUID: $recordUuid is missing a referral.")
      assertThat(moreInfo).isNull()
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
    val recordUuid = transactionTemplate.execute {
      val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
      csipRecord.referral!!.createSaferCustodyScreeningOutcome(
        CsipRequestContext(username = TEST_USER, userDisplayName = TEST_USER_NAME),
        createScreeningOutcomeRequest(),
        outcomeType = givenRandom(ReferenceDataType.OUTCOME_TYPE),
      )
      csipRecordRepository.save(csipRecord).recordUuid
    }

    val request = createScreeningOutcomeRequest()
    val response = createScreeningOutcomeResponseSpec(recordUuid!!, request).expectStatus().is4xxClientError
      .expectBody<ErrorResponse>().returnResult().responseBody!!

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
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = createScreeningOutcomeRequest(recordedBy = "Safer")

    val response = createScreeningOutcome(recordUuid, request)

    // Screening Outcome populated with data from request and context
    with(response) {
      assertThat(reasonForDecision).isEqualTo(request.reasonForDecision)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(date).isEqualTo(request.date)
      assertThat(recordedBy).isEqualTo(request.recordedBy)
      assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
    }

    // Audit event saved
    with(auditEventRepository.findAll().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Safer custody screening outcome added to referral")
      assertThat(affectedComponents).containsOnly(AffectedComponent.SaferCustodyScreeningOutcome)
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByCapturedName).isEqualTo(TEST_USER_NAME)
      assertThat(source).isEqualTo(Source.DPS)
      assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
    }

    // person.csip.record.updated domain event published
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveCsipDomainEventOnQueue()
    assertThat(event).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        eventType = DomainEventType.CSIP_UPDATED.eventType,
        additionalInformation = CsipAdditionalInformation(
          recordUuid = recordUuid,
          affectedComponents = setOf(AffectedComponent.SaferCustodyScreeningOutcome),
          source = Source.DPS,
        ),
        description = "Safer custody screening outcome added to referral",
        version = 1,
        occurredAt = event.occurredAt,
        detailUrl = "http://localhost:8080/csip-records/$recordUuid",
        personReference = PersonReference.withPrisonNumber(prisonNumber),
      ),
    )
  }

  @Test
  fun `create safer custody screening outcome via NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("S1234CN")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = createScreeningOutcomeRequest()

    val response = createScreeningOutcome(recordUuid, request, Source.NOMIS, NOMIS_SYS_USER)

    // Screening Outcome populated with data from request and context
    with(response) {
      assertThat(reasonForDecision).isEqualTo(request.reasonForDecision)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(date).isEqualTo(request.date)
      assertThat(recordedBy).isEqualTo(request.recordedBy)
      assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
    }

    // Audit event saved
    with(auditEventRepository.findAll().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Safer custody screening outcome added to referral")
      assertThat(affectedComponents).containsOnly(AffectedComponent.SaferCustodyScreeningOutcome)
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(actionedByCapturedName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
      assertThat(source).isEqualTo(Source.NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }

    // person.csip.record.updated domain event published
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveCsipDomainEventOnQueue()
    assertThat(event).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        eventType = DomainEventType.CSIP_UPDATED.eventType,
        additionalInformation = CsipAdditionalInformation(
          recordUuid = recordUuid,
          affectedComponents = setOf(AffectedComponent.SaferCustodyScreeningOutcome),
          source = Source.NOMIS,
        ),
        description = "Safer custody screening outcome added to referral",
        version = 1,
        occurredAt = event.occurredAt,
        detailUrl = "http://localhost:8080/csip-records/$recordUuid",
        personReference = PersonReference.withPrisonNumber(prisonNumber),
      ),
    )
  }

  private fun createScreeningOutcomeResponseSpec(
    recordUuid: UUID,
    request: CreateSaferCustodyScreeningOutcomeRequest,
    source: Source = Source.DPS,
    username: String = TEST_USER,
  ) = webTestClient.post().uri("/csip-records/$recordUuid/referral/safer-custody-screening")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), isUserToken = true))
    .headers(setCsipRequestContext(source, username)).exchange()

  private fun createScreeningOutcome(
    recordUuid: UUID,
    request: CreateSaferCustodyScreeningOutcomeRequest,
    source: Source = Source.DPS,
    username: String = TEST_USER,
  ) = createScreeningOutcomeResponseSpec(recordUuid, request, source, username)
    .expectStatus().isCreated
    .expectBody<SaferCustodyScreeningOutcome>()
    .returnResult().responseBody!!

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
        "Validation failure(s): Outcome Type code must be <= 12 characters",
      ),
      Arguments.of(
        createScreeningOutcomeRequest(reasonForDecision = "n".repeat(4001)),
        "Validation failure(s): Reason for Decision must be <= 4000 characters",
      ),
      Arguments.of(
        createScreeningOutcomeRequest(recordedBy = "n".repeat(101)),
        "Validation failure(s): Recorded by username must be <= 100 characters",
      ),
      Arguments.of(
        createScreeningOutcomeRequest(recordedByDisplayName = "n".repeat(256)),
        "Validation failure(s): Recorded by display name must be <= 255 characters",
      ),
    )
  }
}
