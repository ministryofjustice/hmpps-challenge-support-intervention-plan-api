package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpsertDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpsertSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.verifyAgainst
import java.time.Duration.between
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.time.LocalDateTime.now
import java.util.SortedSet
import java.util.UUID

class RetrieveCsipRecordIntTest : IntegrationTestBase() {
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = getCsipRecordResponseSpec(UUID.randomUUID(), role).errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}").exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `404 not found - when no csip record exists with matching uuid`() {
    val notExistingUuid = UUID.randomUUID()
    val response = getCsipRecordResponseSpec(notExistingUuid).errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: CSIP Record not found")
      assertThat(developerMessage).isEqualTo("CSIP Record not found with identifier $notExistingUuid")
      assertThat(moreInfo).isNull()
    }
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_CSIP_UI, ROLE_NOMIS])
  fun `200 ok - returns matching CSIP record`(role: String) {
    val record = dataSetup(generateCsipRecord().withCompletedReferral().withPlan()) {
      val referral = requireNotNull(it.referral).withContributoryFactor()
        .withSaferCustodyScreeningOutcome().withInvestigation().withDecisionAndActions()
      requireNotNull(referral.investigation).withInterview("A N Other")
      val plan = requireNotNull(it.plan).withNeed("One need").withNeed("Another need")
        .withReview(LocalDate.now().minusDays(1))
        .withReview(actions = sortedSetOf(ReviewAction.REMAIN_ON_CSIP))
      requireNotNull(plan.reviews().random()).withAttendee()
      it
    }

    val response = getCsipRecord(record.id, role)
    response.verifyAgainst(record)
    assertThat(between(response.plan!!.createdAt, now())).isLessThan(ofSeconds(2))
  }

  @Test
  fun `screening outcome and decision history correctly returned`() {
    val referral = dataSetup(generateCsipRecord().withCompletedReferral()) {
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome(
        givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "NFA"),
      ).withDecisionAndActions(
        outcome = givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "ACC"),
      )
    }

    val rdSupplier: (ReferenceDataType, String) -> ReferenceData = { type, code ->
      requireNotNull(referenceDataRepository.findByKey(ReferenceDataKey(type, code)))
    }

    val screeningOutcome = requireNotNull(referral.saferCustodyScreeningOutcome)
    val decision = requireNotNull(referral.decisionAndActions)
    screeningOutcome.update(screeningOutcomeRequest("CUR"), rdSupplier)
    decision.upsert(
      upsertDecisionActionsRequest("NFA"),
      givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "NFA"),
      decision.signedOffBy,
    )
    csipRecordRepository.save(referral.csipRecord)

    val saved = retrieveFullCsip(referral.id)
    val newScreeningOutcome = requireNotNull(saved.referral!!.saferCustodyScreeningOutcome)
    val newDecision = requireNotNull(saved.referral!!.decisionAndActions)
    newScreeningOutcome.update(screeningOutcomeRequest("OPE"), rdSupplier)
    newDecision.upsert(
      upsertDecisionActionsRequest("CUR"),
      givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "CUR"),
      decision.signedOffBy,
    )
    csipRecordRepository.save(saved)

    val response = getCsipRecord(referral.id)
    response.verifyAgainst(saved)

    val screeningHistory = requireNotNull(response.referral.saferCustodyScreeningOutcome?.history)
    assertThat(screeningHistory).hasSize(2)
    assertThat(screeningHistory[0].outcome.code).isEqualTo("NFA")
    assertThat(screeningHistory[1].outcome.code).isEqualTo("CUR")

    val decisionHistory = requireNotNull(response.referral.decisionAndActions?.history)
    assertThat(decisionHistory).hasSize(2)
    assertThat(decisionHistory[0].outcome?.code).isEqualTo("ACC")
    assertThat(decisionHistory[1].outcome?.code).isEqualTo("NFA")
  }

  fun getCsipRecordResponseSpec(recordUuid: UUID, role: String? = ROLE_CSIP_UI): WebTestClient.ResponseSpec = webTestClient.get()
    .uri("/csip-records/$recordUuid")
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .exchange()

  fun getCsipRecord(recordUuid: UUID, role: String = ROLE_CSIP_UI): CsipRecord = getCsipRecordResponseSpec(recordUuid, role).successResponse()

  private fun retrieveFullCsip(id: UUID): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord = transactionTemplate.execute {
    val csip = csipRecordRepository.findById(id)
    csip?.referral?.contributoryFactors()
    csip
  }!!

  private fun screeningOutcomeRequest(
    outcomeTypeCode: String,
    reasonForDecision: String = "Reason for $outcomeTypeCode",
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

  private fun upsertDecisionActionsRequest(
    outcomeTypeCode: String,
    outcomeSignedOffByRoleCode: String = "CUSTMAN",
    actions: SortedSet<DecisionAction> = sortedSetOf(),
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
}
