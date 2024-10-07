package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
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
      assertThat(userMessage).isEqualTo("Validation failure: Outcome Type code must be <= 12 characters")
    }
  }

  @Test
  fun `400 bad request - invalid Outcome Type code`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val request = upsertDecisionActionsRequest(outcomeTypeCode = "WRONG_CODE", outcomeSignedOffByRoleCode = "CUR")
    val response = upsertDecisionResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: DECISION_OUTCOME_TYPE is invalid")
      assertThat(developerMessage).isEqualTo("Details => DECISION_OUTCOME_TYPE:WRONG_CODE")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - inactive Outcome Type code`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val request = upsertDecisionActionsRequest(outcomeTypeCode = "OT_INACT", outcomeSignedOffByRoleCode = "CUR")
    val response = upsertDecisionResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: DECISION_OUTCOME_TYPE is not active")
      assertThat(developerMessage).isEqualTo("Details => DECISION_OUTCOME_TYPE:OT_INACT")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - inactive Outcome signed off by role code`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val request = upsertDecisionActionsRequest(outcomeTypeCode = "CUR", outcomeSignedOffByRoleCode = "DSR_INACT")
    val response = upsertDecisionResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)

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
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val request = upsertDecisionActionsRequest(outcomeTypeCode = "CUR", outcomeSignedOffByRoleCode = "WRONG_CODE")
    val response = upsertDecisionResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)

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
    val record = givenCsipRecord(generateCsipRecord())
    val request = upsertDecisionActionsRequest()

    val response = upsertDecisionResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)

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
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val request = upsertDecisionActionsRequest("CUR", "OTHER")

    val response = upsertDecisionActions(record.id, request, status = HttpStatus.CREATED)
    response.verifyAgainst(request)
    val csip = requireNotNull(csipRecordRepository.getCsipRecord(record.id))
    val decision = requireNotNull(csip.referral?.decisionAndActions)
    verifyAudit(decision, RevisionType.ADD, setOf(CsipComponent.DECISION_AND_ACTIONS))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  @Test
  fun `create decision and actions all actions true`() {
    val record = dataSetup(generateCsipRecord()) { it.withReferral() }

    val request = upsertDecisionActionsRequest(
      "CUR",
      "CUSTMAN",
      DecisionAction.entries.toSet(),
    )

    val response = upsertDecisionActions(record.id, request, status = HttpStatus.CREATED)
    response.verifyAgainst(request)
    val csip = requireNotNull(csipRecordRepository.getCsipRecord(record.id))
    val decision = requireNotNull(csip.referral?.decisionAndActions)
    verifyAudit(decision, RevisionType.ADD, setOf(CsipComponent.DECISION_AND_ACTIONS))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  @Test
  fun `create decision and actions via DPS UI`() {
    val record = dataSetup(generateCsipRecord()) { it.withReferral() }
    val request = upsertDecisionActionsRequest()

    val response = upsertDecisionActions(record.id, request, status = HttpStatus.CREATED)
    response.verifyAgainst(request)

    val csip = requireNotNull(csipRecordRepository.getCsipRecord(record.id))
    val decision = requireNotNull(csip.referral?.decisionAndActions)
    assertThat(decision.createdBy).isEqualTo(TEST_USER)
    assertThat(decision.createdByDisplayName).isEqualTo(TEST_USER_NAME)

    verifyAudit(decision, RevisionType.ADD, setOf(CsipComponent.DECISION_AND_ACTIONS))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  @Test
  fun `200 ok - no changes made to decisions`() {
    val record = dataSetup(generateCsipRecord().withReferral()) {
      requireNotNull(it.referral).withDecisionAndActions(
        outcome = givenRandom(ReferenceDataType.DECISION_OUTCOME_TYPE),
        signedOffBy = givenRandom(ReferenceDataType.DECISION_SIGNER_ROLE),
        actions = setOf(DecisionAction.OPEN_CSIP_ALERT),
        conclusion = "a conclusion",
        recordedBy = "outcomeRecordedBy",
        recordedByDisplayName = "outcomeRecordedByDisplayName",
        date = LocalDate.now(),
        nextSteps = "next steps",
      )
      it
    }

    val decision = requireNotNull(record.referral?.decisionAndActions)
    val request = upsertDecisionActionsRequest(decision.outcome!!.code, decision.signedOffBy!!.code, decision.actions)

    val response = upsertDecisionActions(record.id, request, status = HttpStatus.OK)
    response.verifyAgainst(request)

    verifyAudit(
      record.referral!!.decisionAndActions!!,
      RevisionType.ADD,
      setOf(CsipComponent.DECISION_AND_ACTIONS, CsipComponent.REFERRAL, CsipComponent.RECORD),
      nomisContext().copy(source = DPS),
    )

    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsTestQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 ok - decision updated`() {
    val record = dataSetup(generateCsipRecord().withReferral()) {
      requireNotNull(it.referral).withDecisionAndActions()
      it
    }

    val decision = requireNotNull(record.referral?.decisionAndActions)
    val request = upsertDecisionActionsRequest(
      decision.outcome!!.code,
      decision.signedOffBy!!.code,
      setOf(DecisionAction.UNIT_OR_CELL_MOVE),
    )

    val response = upsertDecisionActions(record.id, request, status = HttpStatus.OK)
    response.verifyAgainst(request)

    verifyAudit(record.referral!!.decisionAndActions!!, RevisionType.MOD, setOf(CsipComponent.DECISION_AND_ACTIONS))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  fun urlToTest(recordUuid: UUID) = "/csip-records/$recordUuid/referral/decision-and-actions"

  private fun upsertDecisionActionsRequest(
    outcomeTypeCode: String = "CUR",
    outcomeSignedOffByRoleCode: String = "CUSTMAN",
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

  private fun upsertDecisionResponseSpec(
    recordUuid: UUID,
    request: UpsertDecisionAndActionsRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri(urlToTest(recordUuid))
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role))).exchange()

  fun upsertDecisionActions(
    recordUuid: UUID,
    request: UpsertDecisionAndActionsRequest,
    username: String = TEST_USER,
    role: String? = ROLE_CSIP_UI,
    status: HttpStatus,
  ) = upsertDecisionResponseSpec(recordUuid, request, username, role)
    .successResponse<DecisionAndActions>(status)

  private fun DecisionAndActions.verifyAgainst(request: UpsertDecisionAndActionsRequest) {
    assertThat(conclusion).isEqualTo(request.conclusion)
    assertThat(outcome?.code).isEqualTo(request.outcomeTypeCode)
    assertThat(signedOffByRole?.code).isEqualTo(request.signedOffByRoleCode)
    assertThat(recordedBy).isEqualTo(request.recordedBy)
    assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
    assertThat(date).isEqualTo(request.date)
    assertThat(nextSteps).isEqualTo(request.nextSteps)
    assertThat(actions).containsExactlyInAnyOrderElementsOf(request.actions)
    assertThat(actionOther).isEqualTo(request.actionOther)
  }
}
