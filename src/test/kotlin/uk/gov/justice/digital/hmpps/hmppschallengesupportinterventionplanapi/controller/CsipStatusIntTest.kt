package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CsipRecordService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.set
import java.time.LocalDate

class CsipStatusIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var service: CsipRecordService

  @Test
  fun `csip status - CsipClosed`() {
    val record = dataSetup(generateCsipRecord()) { csip ->
      csip.withCompletedReferral().withPlan()
      val plan = requireNotNull(csip.plan)
      plan.withReview(actions = setOf(ReviewAction.CSIP_UPDATED))
        .withReview(actions = setOf(ReviewAction.CLOSE_CSIP), csipClosedDate = LocalDate.now())
      csip
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.CSIP_CLOSED.name)
    assertThat(saved.plan?.nextCaseReviewDate).isNull()
  }

  @Test
  fun `csip status - CsipOpen`() {
    val nextReviewDate = LocalDate.now().plusWeeks(6)
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral().withPlan()
      val plan = requireNotNull(it.plan)
      plan.withReview(actions = setOf(ReviewAction.CSIP_UPDATED))
        .withReview(actions = setOf(ReviewAction.REMAIN_ON_CSIP), nextReviewDate = nextReviewDate)
      it
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.CSIP_OPEN.name)
    assertThat(saved.plan?.nextCaseReviewDate).isEqualTo(nextReviewDate)
  }

  @Test
  fun `csip status - AwaitingDecision`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "OPE"))
        .withInvestigation()
      it
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.AWAITING_DECISION.name)
  }

  @Test
  fun `csip status - AcctSupport 1`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "ACC"))
        .withInvestigation()
      it
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.ACCT_SUPPORT.name)
  }

  @Test
  fun `csip status - AcctSupport 2`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "OPE"))
        .withInvestigation()
        .withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "ACC"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.ACCT_SUPPORT.name)
  }

  @Test
  fun `csip status - PlanPending 1`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "CUR"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.PLAN_PENDING.name)
  }

  @Test
  fun `csip status - PlanPending 2`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "OPE"))
        .withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "CUR"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.PLAN_PENDING.name)
  }

  @Test
  fun `csip status - InvestigationPending`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "OPE"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.INVESTIGATION_PENDING.name)
  }

  @Test
  fun `csip status - NoFurtherAction 1`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "NFA"))
        .withInvestigation()
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.NO_FURTHER_ACTION.name)
  }

  @Test
  fun `csip status - NoFurtherAction 2`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "OPE"))
        .withInvestigation()
        .withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "NFA"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.NO_FURTHER_ACTION.name)
  }

  @Test
  fun `csip status - NoFurtherAction 3`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "CUR"))
        .withInvestigation()
        .withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "NFA"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.NO_FURTHER_ACTION.name)
  }

  @Test
  fun `csip status - Outside of CSIP`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "CUR"))
        .withInvestigation()
        .withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "WIN"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.SUPPORT_OUTSIDE_CSIP.name)
  }

  @Test
  fun `csip status - ACCT Support`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "CUR"))
        .withInvestigation()
        .withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "ACC"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.ACCT_SUPPORT.name)
  }

  @Test
  fun `csip status - ReferralSubmitted`() {
    val record = dataSetup(generateCsipRecord()) { it.withCompletedReferral() }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.REFERRAL_SUBMITTED.name)
  }

  @Test
  fun `csip status - ReferralPending`() {
    val record = dataSetup(generateCsipRecord()) { it.withReferral() }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.REFERRAL_PENDING.name)
  }

  @Test
  fun `test updates update status`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())

    var saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.REFERRAL_PENDING.name)

    record.setReferralComplete()
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.REFERRAL_SUBMITTED.name)

    record.addSaferCustodyScreeningOutcome()
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.INVESTIGATION_PENDING.name)

    record.addInvestigation()
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.AWAITING_DECISION.name)

    record.changeScreeningOutcome("CUR")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.PLAN_PENDING.name)

    record.changeDecision("ACC")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.ACCT_SUPPORT.name)

    record.changeDecision("WIN")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.SUPPORT_OUTSIDE_CSIP.name)

    record.changeDecision("NFA")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.NO_FURTHER_ACTION.name)

    record.changeScreeningOutcome("OPE")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.NO_FURTHER_ACTION.name)

    record.changeScreeningOutcome("CUR")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.NO_FURTHER_ACTION.name)

    record.changeScreeningOutcome("OPE")
    record.changeDecision("CUR")
    record.addPlan()
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.CSIP_OPEN.name)

    record.addReview(ReviewAction.CSIP_UPDATED, ReviewAction.CASE_NOTE, ReviewAction.RESPONSIBLE_PEOPLE_INFORMED)
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.CSIP_OPEN.name)

    record.changeDecision("NFA")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.NO_FURTHER_ACTION.name)

    record.addReview(ReviewAction.CASE_NOTE, ReviewAction.CLOSE_CSIP)
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status.code).isEqualTo(CsipStatus.CSIP_CLOSED.name)
  }

  private fun CsipRecord.setReferralComplete() = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(id)
    requireNotNull(csip.referral).apply {
      set(::referralComplete, true)
      set(::referralCompletedDate, LocalDate.now())
      set(::referralCompletedBy, "referralCompletedBy")
      set(::referralCompletedByDisplayName, "referralCompletedByDisplayName")
    }
  }

  private fun CsipRecord.addSaferCustodyScreeningOutcome() = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(id)
    requireNotNull(csip.referral).withSaferCustodyScreeningOutcome(
      outcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "OPE"),
    )
  }

  private fun CsipRecord.addInvestigation() = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(id)
    requireNotNull(csip.referral).withInvestigation()
  }

  private fun CsipRecord.changeDecision(code: String) = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(id)
    if (csip.referral?.decisionAndActions == null) {
      requireNotNull(csip.referral).withDecisionAndActions()
    }
    requireNotNull(csip.referral!!.decisionAndActions).apply {
      set(::conclusion, "A simple conclusion")
      set(::outcome, givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, code))
    }
  }

  private fun CsipRecord.changeScreeningOutcome(code: String) = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(id)
    requireNotNull(csip.referral!!.saferCustodyScreeningOutcome).apply {
      set(::outcome, givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, code))
    }
  }

  private fun CsipRecord.addPlan() = transactionTemplate.execute {
    csipRecordRepository.getCsipRecord(id).withPlan()
  }

  private fun CsipRecord.addReview(vararg actions: ReviewAction) = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(id)
    requireNotNull(csip.plan).withReview(actions = buildSet { addAll(actions) })
  }
}
