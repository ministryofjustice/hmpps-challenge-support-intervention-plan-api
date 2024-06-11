package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DecisionActionsIntTest(
  @Autowired private val csipRecordRepository: CsipRecordRepository,
  @Autowired private val referenceDataRepository: ReferenceDataRepository,
) : IntegrationTestBase() {
  private val outcomeType = referenceDataRepository.findByDomain(ReferenceDataType.OUTCOME_TYPE).first()
  private val decisionSignerRole = referenceDataRepository.findByDomain(ReferenceDataType.DECISION_SIGNER_ROLE).first()
  private val incidentLocation = referenceDataRepository.findByDomain(ReferenceDataType.INCIDENT_LOCATION).first()
  private val incidentInvolvement = referenceDataRepository.findByDomain(ReferenceDataType.INCIDENT_INVOLVEMENT).first()
  private val refererAreaOfWork = referenceDataRepository.findByDomain(ReferenceDataType.AREA_OF_WORK).first()

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
    val recordUuid = createCsipRecord().recordUuid
    val request = createDecisionActionsRequest()

    val response = webTestClient.post().uri("/csip-records/${recordUuid}/referral/decision-and-actions")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).headers(setCsipRequestContext(Source.DPS, null))
      .exchange().expectStatus().isBadRequest.expectBody(ErrorResponse::class.java).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Could not find non empty username from user_name or username token claims or Username header")
      assertThat(developerMessage).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username was not found`() {
    val recordUuid = createCsipRecord().recordUuid
    val request = createDecisionActionsRequest()

    val response = webTestClient.post().uri("/csip-records/${recordUuid}/referral/decision-and-actions")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = "UNKNOWN", isUserToken = true)).headers(setCsipRequestContext())
      .exchange().expectStatus().isBadRequest.expectBody(ErrorResponse::class.java).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - request body validation failure`() {
    val response = webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/decision-and-actions")
      .bodyValue(createDecisionActionsRequest(outcomeTypeCode = "n".repeat(13)))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext()).exchange().expectStatus().isBadRequest.expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
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
    val response = webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/decision-and-actions")
      .bodyValue(createDecisionActionsRequest(outcomeTypeCode = "WRONG_CODE", outcomeSignedOffByRoleCode = "CUR"))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext()).exchange().expectStatus().isBadRequest.expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: OUTCOME_TYPE code 'WRONG_CODE' does not exist")
      assertThat(developerMessage).isEqualTo("OUTCOME_TYPE code 'WRONG_CODE' does not exist")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - invalid Outcome signed off by role code`() {
    val response = webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/decision-and-actions")
      .bodyValue(createDecisionActionsRequest(outcomeTypeCode = "CUR", outcomeSignedOffByRoleCode = "WRONG_CODE"))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext()).exchange().expectStatus().isBadRequest.expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: OUTCOME_TYPE code 'WRONG_CODE' does not exist")
      assertThat(developerMessage).isEqualTo("OUTCOME_TYPE code 'WRONG_CODE' does not exist")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - CSIP record missing a referral`() {
    val csipRecord = createCsipRecord(withReferral = false)
    val recordUuid = csipRecord.recordUuid

    val response = webTestClient.post().uri("/csip-records/$recordUuid/referral/decision-and-actions")
      .bodyValue(createDecisionActionsRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext()).exchange().expectStatus().isBadRequest.expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
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
    val response = webTestClient.post().uri("/csip-records/$recordUuid/referral/decision-and-actions")
      .bodyValue(createDecisionActionsRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext()).exchange().expectStatus().isNotFound.expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("No resource found failure: Could not find CSIP record with UUID $recordUuid")
      assertThat(developerMessage).isEqualTo("Could not find CSIP record with UUID $recordUuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `409 conflict - CSIP record already has Screening Outcome created`() {
    val csipRecord = createCsipRecord()
    val recordUuid = csipRecord.recordUuid

    csipRecordRepository.save(
      csipRecord.let {
        it.referral!!.createDecisionAndActions(
          decisionOutcome = outcomeType,
          decisionOutcomeSignedOffBy = decisionSignerRole,
          decisionOutcomeDate = LocalDate.now(),
          actionedAt = LocalDateTime.now(),
          decisionOutcomeRecordedBy = "actionedBy",
          decisionOutcomeRecordedByDisplayName = "actionedByDisplayName",
          source = Source.DPS,
          reason = Reason.USER,
          activeCaseLoadId = PRISON_CODE_LEEDS,
          description = "description",
          decisionConclusion = null,
          actionOther = null,
          actionObservationBook = null,
          actionServiceReferral = null,
          actionOpenCsipAlert = null,
          actionSimReferral = null,
          actionUnitOrCellMove = null,
          nextSteps = null,
          actionCsraOrRsraReview = null,
          actionNonAssociationsUpdated = null,
        )
      },
    )

    val response = webTestClient.post().uri("/csip-records/$recordUuid/referral/decision-and-actions")
      .bodyValue(createDecisionActionsRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext()).exchange().expectStatus().is4xxClientError.expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Conflict failure: CSIP Record with UUID: $recordUuid already has a Decision and Actions created.")
      assertThat(developerMessage).isEqualTo("CSIP Record with UUID: $recordUuid already has a Decision and Actions created.")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `create decision and actions via DPS UI`() {
    val recordUuid = createCsipRecord().recordUuid
    val request = createDecisionActionsRequest()

    val response =
      webTestClient.post().uri("/csip-records/$recordUuid/referral/decision-and-actions").bodyValue(request)
        .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
        .headers(setCsipRequestContext()).exchange().expectStatus().isCreated.expectHeader()
        .contentType(MediaType.APPLICATION_JSON).expectBody(DecisionAndActions::class.java)
        .returnResult().responseBody!!

    // Decisions Actions entry populated with data from request and context
    with(response) {
      assertThat(conclusion).isEqualTo(request.conclusion)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(outcomeSignedOffByRole?.code).isEqualTo(request.outcomeSignedOffByRoleCode)
      assertThat(outcomeRecordedBy).isEqualTo(TEST_USER)
      assertThat(outcomeRecordedByDisplayName).isEqualTo(TEST_USER_NAME)
      assertThat(outcomeDate).isEqualTo(LocalDate.now())
      assertThat(nextSteps).isEqualTo(nextSteps)
      assertThat(isActionOpenCsipAlert).isEqualTo(request.isActionOpenCsipAlert)
      assertThat(isActionNonAssociationsUpdated).isEqualTo(isActionNonAssociationsUpdated)
      assertThat(isActionObservationBook).isEqualTo(isActionObservationBook)
      assertThat(isActionUnitOrCellMove).isEqualTo(isActionUnitOrCellMove)
      assertThat(isActionCsraOrRsraReview).isEqualTo(isActionCsraOrRsraReview)
      assertThat(isActionServiceReferral).isEqualTo(isActionServiceReferral)
      assertThat(isActionSimReferral).isEqualTo(isActionSimReferral)
      assertThat(actionOther).isEqualTo(actionOther)
    }

    // Audit event saved
    with(csipRecordRepository.findByRecordUuid(recordUuid)!!.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.UPDATED)
      assertThat(description).isEqualTo("Decision and actions added to referral")
      assertThat(isDecisionAndActionsAffected).isTrue()
      assertThat(isSaferCustodyScreeningOutcomeAffected).isFalse()
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByCapturedName).isEqualTo(TEST_USER_NAME)
      assertThat(source).isEqualTo(Source.DPS)
      assertThat(reason).isEqualTo(Reason.USER)
      assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
    }

    // prisoner-csip.csip-record-updated domain event published
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveCsipDomainEventOnQueue()
    assertThat(event).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        DomainEventType.CSIP_UPDATED.eventType,
        CsipAdditionalInformation(
          url = "http://localhost:8080/csip-records/$recordUuid",
          recordUuid = recordUuid,
          prisonNumber = PRISON_NUMBER,
          isRecordAffected = false,
          isReferralAffected = false,
          isContributoryFactorAffected = false,
          isSaferCustodyScreeningOutcomeAffected = false,
          isInvestigationAffected = false,
          isInterviewAffected = false,
          isDecisionAndActionsAffected = true,
          isPlanAffected = false,
          isIdentifiedNeedAffected = false,
          isReviewAffected = false,
          isAttendeeAffected = false,
          source = Source.DPS,
          reason = Reason.USER,
        ),
        1,
        "Decision and actions added to referral",
        event.occurredAt,
      ),
    )
  }

  @Test
  fun `create decision and actions via NOMIS`() {
    val recordUuid = createCsipRecord().recordUuid
    val request = createDecisionActionsRequest()

    val response =
      webTestClient.post().uri("/csip-records/$recordUuid/referral/decision-and-actions").bodyValue(request)
        .headers(setAuthorisation(roles = listOf(ROLE_NOMIS)))
        .headers(setCsipRequestContext(source = Source.NOMIS, username = NOMIS_SYS_USER)).exchange()
        .expectStatus().isCreated.expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(DecisionAndActions::class.java).returnResult().responseBody!!

    // Screening Outcome populated with data from request and context
    with(response) {
      assertThat(conclusion).isEqualTo(request.conclusion)
      assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
      assertThat(outcomeSignedOffByRole?.code).isEqualTo(request.outcomeSignedOffByRoleCode)
      assertThat(outcomeRecordedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(outcomeRecordedByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
      assertThat(outcomeDate).isEqualTo(LocalDate.now())
      assertThat(nextSteps).isEqualTo(nextSteps)
      assertThat(isActionOpenCsipAlert).isEqualTo(request.isActionOpenCsipAlert)
      assertThat(isActionNonAssociationsUpdated).isEqualTo(isActionNonAssociationsUpdated)
      assertThat(isActionObservationBook).isEqualTo(isActionObservationBook)
      assertThat(isActionUnitOrCellMove).isEqualTo(isActionUnitOrCellMove)
      assertThat(isActionCsraOrRsraReview).isEqualTo(isActionCsraOrRsraReview)
      assertThat(isActionServiceReferral).isEqualTo(isActionServiceReferral)
      assertThat(isActionSimReferral).isEqualTo(isActionSimReferral)
      assertThat(actionOther).isEqualTo(actionOther)
    }

    // Audit event saved
    with(csipRecordRepository.findByRecordUuid(recordUuid)!!.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.UPDATED)
      assertThat(description).isEqualTo("Decision and actions added to referral")
      assertThat(isSaferCustodyScreeningOutcomeAffected).isFalse()
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(actionedByCapturedName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
      assertThat(source).isEqualTo(Source.NOMIS)
      assertThat(reason).isEqualTo(Reason.USER)
      assertThat(activeCaseLoadId).isNull()
    }

    // prisoner-csip.csip-record-updated domain event published
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveCsipDomainEventOnQueue()
    assertThat(event).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        DomainEventType.CSIP_UPDATED.eventType,
        CsipAdditionalInformation(
          url = "http://localhost:8080/csip-records/$recordUuid",
          recordUuid = recordUuid,
          prisonNumber = PRISON_NUMBER,
          isRecordAffected = false,
          isReferralAffected = false,
          isContributoryFactorAffected = false,
          isSaferCustodyScreeningOutcomeAffected = false,
          isInvestigationAffected = false,
          isInterviewAffected = false,
          isDecisionAndActionsAffected = true,
          isPlanAffected = false,
          isIdentifiedNeedAffected = false,
          isReviewAffected = false,
          isAttendeeAffected = false,
          source = Source.NOMIS,
          reason = Reason.USER,
        ),
        1,
        "Decision and actions added to referral",
        event.occurredAt,
      ),
    )
  }

  private fun createCsipRecord(withReferral: Boolean = true) = csipRecordRepository.saveAndFlush(
    CsipRecord(
      recordUuid = UUID.randomUUID(),
      prisonNumber = PRISON_NUMBER,
      prisonCodeWhenRecorded = PRISON_CODE_LEEDS,
      logNumber = "LOG",
      createdAt = LocalDateTime.now(),
      createdBy = "te",
      createdByDisplayName = "Bobbie Shepard",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
    ).let {
      if (withReferral) {
        it.setReferral(
          Referral(
            csipRecord = it,
            incidentDate = LocalDate.now(),
            referredBy = "referredBy",
            referralDate = LocalDate.now(),
            descriptionOfConcern = "descriptionOfConcern",
            knownReasons = "knownReasons",
            otherInformation = "otherInformation",
            saferCustodyTeamInformed = false,
            referralComplete = true,
            referralCompletedBy = "referralCompletedBy",
            referralCompletedByDisplayName = "referralCompletedByDisplayName",
            referralCompletedDate = LocalDate.now(),
            incidentType = outcomeType,
            incidentLocation = incidentLocation,
            refererAreaOfWork = refererAreaOfWork,
            incidentInvolvement = incidentInvolvement,
          ),
        )
      } else {
        it
      }
    },
  )

  private fun createDecisionActionsRequest(
    outcomeTypeCode: String = "CUR",
    outcomeSignedOffByRoleCode: String = "CUR",
  ) = CreateDecisionAndActionsRequest(
    conclusion = null,
    outcomeTypeCode = outcomeTypeCode,
    outcomeSignedOffByRoleCode = outcomeSignedOffByRoleCode,
    outcomeRecordedBy = null,
    outcomeRecordedByDisplayName = null,
    outcomeDate = null,
    nextSteps = null,
    isActionOpenCsipAlert = null,
    isActionNonAssociationsUpdated = null,
    isActionObservationBook = null,
    isActionUnitOrCellMove = null,
    isActionCsraOrRsraReview = null,
    isActionServiceReferral = null,
    isActionSimReferral = null,
    actionOther = null,
  )
}
