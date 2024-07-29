package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DecisionActionIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/decision-and-actions").exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/decision-and-actions")
      .bodyValue(createDecisionActionsRequest()).headers(setAuthorisation()).headers(setCsipRequestContext()).exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - incorrect role`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/decision-and-actions")
      .bodyValue(createDecisionActionsRequest()).headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .headers(setCsipRequestContext()).exchange().expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/decision-and-actions")
      .bodyValue(createDecisionActionsRequest()).headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .headers { it.set(SOURCE, "INVALID") }.exchange().expectStatus().isBadRequest
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(PRISON_NUMBER))
    val recordUuid = csipRecord.recordUuid
    val request = createDecisionActionsRequest()

    val response =
      createDecisionResponseSpec(recordUuid, request, username = null).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Could not find non empty username from user_name or username token claims or Username header")
      assertThat(developerMessage).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username was not found`() {
    val recordUuid = UUID.randomUUID()
    val request = createDecisionActionsRequest()

    val response =
      webTestClient.post().uri("/csip-records/$recordUuid/referral/decision-and-actions").bodyValue(request)
        .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = "UNKNOWN", isUserToken = true))
        .headers(setCsipRequestContext())
        .exchange().errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - request body validation failure`() {
    val recordUuid = UUID.randomUUID()
    val request = createDecisionActionsRequest(outcomeTypeCode = "n".repeat(13))
    val response = createDecisionResponseSpec(recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Outcome Type code must be <= 12 characters")
      assertThat(developerMessage).isEqualTo(
        "Validation failed for argument [1] in public uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource.DecisionAndActionsController.createDecision(java.util.UUID,uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateDecisionAndActionsRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createDecisionAndActionsRequest' on field 'outcomeTypeCode': rejected value [nnnnnnnnnnnnn]; codes [Size.createDecisionAndActionsRequest.outcomeTypeCode,Size.outcomeTypeCode,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createDecisionAndActionsRequest.outcomeTypeCode,outcomeTypeCode]; arguments []; default message [outcomeTypeCode],12,1]; default message [Outcome Type code must be <= 12 characters]] ",
      )
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - invalid Outcome Type code`() {
    val prisonNumber = givenValidPrisonNumber("D1234OT")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val request = createDecisionActionsRequest(outcomeTypeCode = "WRONG_CODE", outcomeSignedOffByRoleCode = "CUR")
    val response = createDecisionResponseSpec(csipRecord.recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: OUTCOME_TYPE is invalid")
      assertThat(developerMessage).isEqualTo("Details => OUTCOME_TYPE:WRONG_CODE")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - inactive Outcome signed off by role code`() {
    val prisonNumber = givenValidPrisonNumber("D1234NS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val request = createDecisionActionsRequest(outcomeTypeCode = "CUR", outcomeSignedOffByRoleCode = "DSR_INACT")
    val response = createDecisionResponseSpec(csipRecord.recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: DECISION_SIGNER_ROLE is not active")
      assertThat(developerMessage).isEqualTo("Details => DECISION_SIGNER_ROLE:DSR_INACT")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - invalid Outcome signed off by role code`() {
    val prisonNumber = givenValidPrisonNumber("D1234IS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val request = createDecisionActionsRequest(outcomeTypeCode = "CUR", outcomeSignedOffByRoleCode = "WRONG_CODE")
    val response = createDecisionResponseSpec(csipRecord.recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: DECISION_SIGNER_ROLE is invalid")
      assertThat(developerMessage).isEqualTo("Details => DECISION_SIGNER_ROLE:WRONG_CODE")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - CSIP record missing a referral`() {
    val prisonNumber = givenValidPrisonNumber("M1234RF")
    val csipRecord = givenCsipRecord(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = createDecisionActionsRequest()

    val response = createDecisionResponseSpec(recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

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
    val request = createDecisionActionsRequest()
    val response = createDecisionResponseSpec(recordUuid, request)
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
    val prisonNumber = givenValidPrisonNumber("E1234CP")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid

    csipRecord.referral!!.createDecisionAndActions(
      CsipRequestContext(
        username = TEST_USER,
        userDisplayName = TEST_USER_NAME,
        activeCaseLoadId = PRISON_CODE_LEEDS,
      ),
      createDecisionActionsRequest(),
    ) { type, code -> referenceDataRepository.getActiveReferenceData(type, code) }
    csipRecordRepository.save(csipRecord)

    val response = createDecisionResponseSpec(recordUuid, createDecisionActionsRequest())
      .expectStatus().is4xxClientError
      .expectBody<ErrorResponse>()
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Conflict failure: Referral already has a Decision and Actions")
      assertThat(developerMessage).isEqualTo("Referral already has a Decision and Actions")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `create decision and actions no signed off by role`() {
    val prisonNumber = givenValidPrisonNumber("D1234NS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = createDecisionActionsRequest("CUR", null)

    val response = createDecisionActions(recordUuid, request)

    // Decisions Actions entry populated with data from request and context
    with(response) {
      assertThat(conclusion).isEqualTo(request.conclusion)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(signedOffByRole).isNull()
      assertThat(recordedBy).isEqualTo("outcomeRecordedBy")
      assertThat(recordedByDisplayName).isEqualTo("outcomeRecordedByDisplayName")
      assertThat(date).isEqualTo(LocalDate.now())
      assertThat(nextSteps).isEqualTo(nextSteps)
      assertThat(actions).isEmpty()
      assertThat(actionOther).isEqualTo(actionOther)
    }
  }

  @Test
  fun `create decision and actions all actions true`() {
    val prisonNumber = givenValidPrisonNumber("D1234AT")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid

    val request = createDecisionActionsRequest(
      "CUR",
      "CUSTMAN",
      DecisionAction.entries.toSet(),
    )

    val response = createDecisionActions(recordUuid, request)

    // Decisions Actions entry populated with data from request and context
    with(response) {
      assertThat(conclusion).isEqualTo(request.conclusion)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(signedOffByRole?.code).isEqualTo(request.signedOffByRoleCode)
      assertThat(recordedBy).isEqualTo("outcomeRecordedBy")
      assertThat(recordedByDisplayName).isEqualTo("outcomeRecordedByDisplayName")
      assertThat(date).isEqualTo(LocalDate.now())
      assertThat(nextSteps).isEqualTo(nextSteps)
      assertThat(actions).containsAll(DecisionAction.entries)
      assertThat(actionOther).isEqualTo(actionOther)
    }
  }

  @Test
  fun `create decision and actions via DPS UI`() {
    val prisonNumber = givenValidPrisonNumber("D1234DU")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = createDecisionActionsRequest()

    val response = createDecisionActions(recordUuid, request)

    // Decisions Actions entry populated with data from request and context
    with(response) {
      assertThat(conclusion).isEqualTo(request.conclusion)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(signedOffByRole?.code).isEqualTo(request.signedOffByRoleCode)
      assertThat(recordedBy).isEqualTo("outcomeRecordedBy")
      assertThat(recordedByDisplayName).isEqualTo("outcomeRecordedByDisplayName")
      assertThat(date).isEqualTo(LocalDate.now())
      assertThat(nextSteps).isEqualTo(nextSteps)
      assertThat(actions).containsExactlyInAnyOrder(*request.actions.toTypedArray())
      assertThat(actionOther).isEqualTo(actionOther)
    }

    // Audit event saved
    with(csipRecordRepository.findByRecordUuid(recordUuid)!!.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Decision and actions added to referral")
      assertThat(affectedComponents).containsOnly(AffectedComponent.DecisionAndActions)
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
          setOf(AffectedComponent.DecisionAndActions),
          source = Source.DPS,
        ),
        version = 1,
        description = "Decision and actions added to referral",
        occurredAt = event.occurredAt,
        detailUrl = "http://localhost:8080/csip-records/$recordUuid",
        personReference = PersonReference.withPrisonNumber(prisonNumber),
      ),
    )
  }

  @Test
  fun `create decision and actions via NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("D1234NS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = createDecisionActionsRequest()

    val response = createDecisionActions(recordUuid, request, source = Source.NOMIS, username = NOMIS_SYS_USER)

    // Screening Outcome populated with data from request and context
    with(response) {
      assertThat(conclusion).isEqualTo(request.conclusion)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(signedOffByRole?.code).isEqualTo(request.signedOffByRoleCode)
      assertThat(recordedBy).isEqualTo("outcomeRecordedBy")
      assertThat(recordedByDisplayName).isEqualTo("outcomeRecordedByDisplayName")
      assertThat(date).isEqualTo(LocalDate.now())
      assertThat(nextSteps).isEqualTo(nextSteps)
      assertThat(actions).containsExactlyInAnyOrder(*request.actions.toTypedArray())
      assertThat(actionOther).isEqualTo(actionOther)
    }

    // Audit event saved
    with(csipRecordRepository.findByRecordUuid(recordUuid)!!.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Decision and actions added to referral")
      assertThat(affectedComponents).doesNotContain(AffectedComponent.SaferCustodyScreeningOutcome)
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
          affectedComponents = setOf(AffectedComponent.DecisionAndActions),
          source = Source.NOMIS,
        ),
        version = 1,
        description = "Decision and actions added to referral",
        occurredAt = event.occurredAt,
        detailUrl = "http://localhost:8080/csip-records/$recordUuid",
        personReference = PersonReference.withPrisonNumber(prisonNumber),
      ),
    )
  }

  private fun createDecisionActionsRequest(
    outcomeTypeCode: String = "CUR",
    outcomeSignedOffByRoleCode: String? = "CUSTMAN",
    actions: Set<DecisionAction> = setOf(),
  ) = CreateDecisionAndActionsRequest(
    conclusion = null,
    outcomeTypeCode = outcomeTypeCode,
    signedOffByRoleCode = outcomeSignedOffByRoleCode,
    recordedBy = "outcomeRecordedBy",
    recordedByDisplayName = "outcomeRecordedByDisplayName",
    date = LocalDate.now(),
    nextSteps = null,
    actionOther = null,
    actions = actions,
  )

  fun createDecisionResponseSpec(
    recordUuid: UUID,
    request: CreateDecisionAndActionsRequest,
    source: Source = Source.DPS,
    username: String? = TEST_USER,
  ): WebTestClient.ResponseSpec = webTestClient.post()
    .uri("/csip-records/$recordUuid/referral/decision-and-actions")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), isUserToken = true))
    .headers(setCsipRequestContext(source = source, username = username)).exchange()

  fun createDecisionActions(
    recordUuid: UUID,
    request: CreateDecisionAndActionsRequest,
    source: Source = Source.DPS,
    username: String = TEST_USER,
  ) = createDecisionResponseSpec(recordUuid, request, source, username)
    .expectStatus().isCreated
    .expectBody<DecisionAndActions>()
    .returnResult().responseBody!!
}