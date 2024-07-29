package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
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

  @Test
  fun `400 bad request - request body validation failure`() {
    val response = webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/safer-custody-screening")
      .bodyValue(createScreeningOutcomeRequest(outcomeTypeCode = "n".repeat(13)))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext()).exchange().expectStatus().isBadRequest.expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Outcome Type code must be <= 12 characters")
      assertThat(developerMessage).isEqualTo(
        "Validation failed for argument [1] in public uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SaferCustodyScreeningOutcome uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource.SaferCustodyScreeningOutcomesController.createScreeningOutcome(java.util.UUID,uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createSaferCustodyScreeningOutcomeRequest' on field 'outcomeTypeCode': rejected value [nnnnnnnnnnnnn]; codes [Size.createSaferCustodyScreeningOutcomeRequest.outcomeTypeCode,Size.outcomeTypeCode,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createSaferCustodyScreeningOutcomeRequest.outcomeTypeCode,outcomeTypeCode]; arguments []; default message [outcomeTypeCode],12,1]; default message [Outcome Type code must be <= 12 characters]] ",
      )
      assertThat(moreInfo).isNull()
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
        outcomeType = givenRandom(ReferenceDataType.OUTCOME_TYPE),
        date = LocalDate.now(),
        reasonForDecision = "reasonForDecision",
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
    val request = createScreeningOutcomeRequest()

    val response = createScreeningOutcome(recordUuid, request)

    // Screening Outcome populated with data from request and context
    with(response) {
      assertThat(reasonForDecision).isEqualTo(request.reasonForDecision)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(date).isEqualTo(request.date)
      assertThat(recordedBy).isEqualTo(TEST_USER)
      assertThat(recordedByDisplayName).isEqualTo(TEST_USER_NAME)
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
      assertThat(recordedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(recordedByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
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

  private fun createScreeningOutcomeRequest(outcomeTypeCode: String = "CUR") =
    CreateSaferCustodyScreeningOutcomeRequest(
      outcomeTypeCode = outcomeTypeCode,
      date = LocalDate.now(),
      reasonForDecision = "alia",
    )

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
}
