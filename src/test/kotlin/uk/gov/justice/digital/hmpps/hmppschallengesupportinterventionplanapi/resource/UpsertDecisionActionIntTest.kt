package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.util.UUID

class UpsertDecisionActionIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @Test
  fun `401 unauthorised`() {
    webTestClient.put().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = upsertDecisionResponseSpec(UUID.randomUUID(), upsertDecisionActionsRequest(), role = role)
      .errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.put().uri(urlToTest(UUID.randomUUID()))
      .bodyValue(upsertDecisionActionsRequest()).headers(setAuthorisation())
      .headers { it.set(SOURCE, "INVALID") }.exchange().errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: No enum constant uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.INVALID")
      assertThat(developerMessage).isEqualTo("No enum constant uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.INVALID")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(PRISON_NUMBER))
    val recordUuid = csipRecord.recordUuid
    val request = upsertDecisionActionsRequest()

    val response = upsertDecisionResponseSpec(recordUuid, request, username = null)
      .errorResponse(HttpStatus.BAD_REQUEST)

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
    val request = upsertDecisionActionsRequest()

    val response = upsertDecisionResponseSpec(recordUuid, request, username = "UNKNOWN")
      .errorResponse(HttpStatus.BAD_REQUEST)

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
    val request = upsertDecisionActionsRequest(outcomeTypeCode = "n".repeat(13))
    val response = upsertDecisionResponseSpec(recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Outcome Type code must be <= 12 characters")
    }
  }

  @Test
  fun `400 bad request - invalid Outcome Type code`() {
    val prisonNumber = givenValidPrisonNumber("D1234OT")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val request = upsertDecisionActionsRequest(outcomeTypeCode = "WRONG_CODE", outcomeSignedOffByRoleCode = "CUR")
    val response = upsertDecisionResponseSpec(csipRecord.recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: OUTCOME_TYPE is invalid")
      assertThat(developerMessage).isEqualTo("Details => OUTCOME_TYPE:WRONG_CODE")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - inactive Outcome Type code`() {
    val prisonNumber = givenValidPrisonNumber("D1234OT")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val request = upsertDecisionActionsRequest(outcomeTypeCode = "OT_INACT", outcomeSignedOffByRoleCode = "CUR")
    val response = upsertDecisionResponseSpec(csipRecord.recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: OUTCOME_TYPE is not active")
      assertThat(developerMessage).isEqualTo("Details => OUTCOME_TYPE:OT_INACT")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - inactive Outcome signed off by role code`() {
    val prisonNumber = givenValidPrisonNumber("D1234NS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val request = upsertDecisionActionsRequest(outcomeTypeCode = "CUR", outcomeSignedOffByRoleCode = "DSR_INACT")
    val response = upsertDecisionResponseSpec(csipRecord.recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

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
    val request = upsertDecisionActionsRequest(outcomeTypeCode = "CUR", outcomeSignedOffByRoleCode = "WRONG_CODE")
    val response = upsertDecisionResponseSpec(csipRecord.recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

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
    val request = upsertDecisionActionsRequest()

    val response = upsertDecisionResponseSpec(recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record is missing a referral.")
      assertThat(developerMessage).isEqualTo("CSIP Record is missing a referral.")
      assertThat(moreInfo).isEqualTo(recordUuid.toString())
    }
  }

  @Test
  fun `404 not found - CSIP record not found`() {
    val recordUuid = UUID.randomUUID()
    val request = upsertDecisionActionsRequest()
    val response = upsertDecisionResponseSpec(recordUuid, request).errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: CSIP Record not found")
      assertThat(developerMessage).isEqualTo("CSIP Record not found with identifier $recordUuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `create decision and actions no signed off by role`() {
    val prisonNumber = givenValidPrisonNumber("D1234NS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = upsertDecisionActionsRequest("CUR", null)

    val response = upsertDecisionActions(recordUuid, request, status = HttpStatus.CREATED)

    response.verifyAgainst(request)
  }

  @Test
  fun `create decision and actions all actions true`() {
    val prisonNumber = givenValidPrisonNumber("D1234AT")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid

    val request = upsertDecisionActionsRequest(
      "CUR",
      "CUSTMAN",
      DecisionAction.entries.toSet(),
    )

    val response = upsertDecisionActions(recordUuid, request, status = HttpStatus.CREATED)

    response.verifyAgainst(request)
  }

  @Test
  fun `create decision and actions via DPS UI`() {
    val prisonNumber = givenValidPrisonNumber("D1234DU")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = upsertDecisionActionsRequest()

    val response = upsertDecisionActions(recordUuid, request, status = HttpStatus.CREATED)
    response.verifyAgainst(request)

    val csip = requireNotNull(csipRecordRepository.getCsipRecord(csipRecord.recordUuid))
    val decision = requireNotNull(csip.referral?.decisionAndActions)
    assertThat(decision.createdBy).isEqualTo(TEST_USER)
    assertThat(decision.createdByDisplayName).isEqualTo(TEST_USER_NAME)
    assertThat(csip.lastModifiedBy).isEqualTo(TEST_USER)
    assertThat(csip.lastModifiedByDisplayName).isEqualTo(TEST_USER_NAME)

    verifyAudit(
      csipRecord,
      UPDATE,
      setOf(AffectedComponent.DecisionAndActions, AffectedComponent.Referral, AffectedComponent.Record),
    )

    verifyDomainEvents(
      prisonNumber,
      recordUuid,
      setOf(AffectedComponent.DecisionAndActions),
      setOf(CSIP_UPDATED),
    )
  }

  @Test
  fun `create decision and actions via NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("D1234CD")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = upsertDecisionActionsRequest()

    val response = upsertDecisionActions(
      recordUuid,
      request,
      source = NOMIS,
      username = NOMIS_SYS_USER,
      role = ROLE_NOMIS,
      status = HttpStatus.CREATED,
    )

    response.verifyAgainst(request)

    val csip = requireNotNull(csipRecordRepository.getCsipRecord(csipRecord.recordUuid))
    val decision = requireNotNull(csip.referral?.decisionAndActions)
    assertThat(decision.createdBy).isEqualTo(NOMIS_SYS_USER)
    assertThat(decision.createdByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
    assertThat(csip.lastModifiedBy).isEqualTo(NOMIS_SYS_USER)
    assertThat(csip.lastModifiedByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)

    verifyAudit(
      csipRecord,
      UPDATE,
      setOf(AffectedComponent.DecisionAndActions, AffectedComponent.Referral, AffectedComponent.Record),
      nomisContext(),
    )

    verifyDomainEvents(
      prisonNumber,
      recordUuid,
      setOf(AffectedComponent.DecisionAndActions),
      setOf(CSIP_UPDATED),
      source = NOMIS,
    )
  }

  @Test
  fun `200 ok - no changes made to decisions`() {
    val prisonNumber = givenValidPrisonNumber("D1234NC")
    val csipRecord = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
      requireNotNull(csip.referral).withDecisionAndActions(
        outcome = givenRandom(ReferenceDataType.OUTCOME_TYPE),
        signedOffBy = givenRandom(ReferenceDataType.DECISION_SIGNER_ROLE),
        actions = setOf(DecisionAction.OpenCsipAlert),
        conclusion = "a conclusion",
        recordedBy = "outcomeRecordedBy",
        recordedByDisplayName = "outcomeRecordedByDisplayName",
        date = LocalDate.now(),
        nextSteps = "next steps",
      )
      csip
    }!!

    val decision = requireNotNull(csipRecord.referral?.decisionAndActions)
    val request = upsertDecisionActionsRequest(decision.outcome.code, decision.signedOffBy?.code, decision.actions)

    val response = upsertDecisionActions(csipRecord.recordUuid, request, status = HttpStatus.OK)
    response.verifyAgainst(request)

    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 ok - decision updated`() {
    val prisonNumber = givenValidPrisonNumber("D1234UD")
    val csipRecord = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
      requireNotNull(csip.referral).withDecisionAndActions()
      csip
    }!!

    val decision = requireNotNull(csipRecord.referral?.decisionAndActions)
    val request = upsertDecisionActionsRequest(
      decision.outcome.code,
      decision.signedOffBy?.code,
      setOf(DecisionAction.UnitOrCellMove),
    )

    val response = upsertDecisionActions(csipRecord.recordUuid, request, status = HttpStatus.OK)
    response.verifyAgainst(request)

    verifyAudit(
      csipRecord,
      UPDATE,
      setOf(AffectedComponent.DecisionAndActions, AffectedComponent.Referral, AffectedComponent.Record),
    )

    verifyDomainEvents(
      prisonNumber,
      csipRecord.recordUuid,
      setOf(AffectedComponent.DecisionAndActions),
      setOf(CSIP_UPDATED),
    )
  }

  private fun upsertDecisionActionsRequest(
    outcomeTypeCode: String = "CUR",
    outcomeSignedOffByRoleCode: String? = "CUSTMAN",
    actions: Set<DecisionAction> = setOf(),
  ) = UpsertDecisionAndActionsRequest(
    conclusion = "a conclusion",
    outcomeTypeCode = outcomeTypeCode,
    signedOffByRoleCode = outcomeSignedOffByRoleCode,
    recordedBy = "outcomeRecordedBy",
    recordedByDisplayName = "outcomeRecordedByDisplayName",
    date = LocalDate.now(),
    nextSteps = "next steps",
    actionOther = null,
    actions = actions,
  )

  fun urlToTest(recordUuid: UUID) = "/csip-records/$recordUuid/referral/decision-and-actions"

  private fun upsertDecisionResponseSpec(
    recordUuid: UUID,
    request: UpsertDecisionAndActionsRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri(urlToTest(recordUuid))
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username)).exchange()

  fun upsertDecisionActions(
    recordUuid: UUID,
    request: UpsertDecisionAndActionsRequest,
    source: Source = DPS,
    username: String = TEST_USER,
    role: String? = ROLE_CSIP_UI,
    status: HttpStatus,
  ) = upsertDecisionResponseSpec(recordUuid, request, source, username, role)
    .successResponse<DecisionAndActions>(status)

  private fun DecisionAndActions.verifyAgainst(request: UpsertDecisionAndActionsRequest) {
    assertThat(conclusion).isEqualTo(request.conclusion)
    assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
    assertThat(signedOffByRole?.code).isEqualTo(request.signedOffByRoleCode)
    assertThat(recordedBy).isEqualTo(request.recordedBy)
    assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
    assertThat(date).isEqualTo(request.date)
    assertThat(nextSteps).isEqualTo(request.nextSteps)
    assertThat(actions).containsExactlyInAnyOrderElementsOf(request.actions)
    assertThat(actionOther).isEqualTo(request.actionOther)
  }
}
