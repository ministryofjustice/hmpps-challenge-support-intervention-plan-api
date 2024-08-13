package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CsipRecordService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.set
import java.time.LocalDate

class CsipStatusIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var service: CsipRecordService

  @Test
  fun `csip status - CsipClosed`() {
    val prisonNumber = "S1234TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) { csip ->
      csip.withCompletedReferral().withPlan()
      val plan = requireNotNull(csip.plan)
      plan.withReview(actions = setOf(ReviewAction.CSIP_UPDATED))
        .withReview(actions = setOf(ReviewAction.CLOSE_CSIP))
      csip
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.CSIP_CLOSED)
  }

  @Test
  fun `csip status - CsipOpen`() {
    val prisonNumber = "S1235TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withCompletedReferral().withPlan()
      val plan = requireNotNull(it.plan)
      plan.withReview(actions = setOf(ReviewAction.CSIP_UPDATED))
        .withReview(actions = setOf(ReviewAction.REMAIN_ON_CSIP))
      it
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.CSIP_OPEN)
  }

  @Test
  fun `csip status - AwaitingDecision`() {
    val prisonNumber = "S1236TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"))
        .withDecisionAndActions(conclusion = null)
      it
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.AWAITING_DECISION)
  }

  @Test
  fun `csip status - AcctSupport 1`() {
    val prisonNumber = "S1237TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "ACC"))
      it
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.ACCT_SUPPORT)
  }

  @Test
  fun `csip status - AcctSupport 2`() {
    val prisonNumber = "S1238TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"))
        .withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "ACC"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.ACCT_SUPPORT)
  }

  @Test
  fun `csip status - PlanPending 1`() {
    val prisonNumber = "S1239TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "CUR"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.PLAN_PENDING)
  }

  @Test
  fun `csip status - PlanPending 2`() {
    val prisonNumber = "S1240TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"))
        .withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "CUR"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.PLAN_PENDING)
  }

  @Test
  fun `csip status - InvestigationPending`() {
    val prisonNumber = "S1241TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.INVESTIGATION_PENDING)
  }

  @Test
  fun `csip status - NoFurtherAction 1`() {
    val prisonNumber = "S1242TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "NFA"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.NO_FURTHER_ACTION)
  }

  @Test
  fun `csip status - NoFurtherAction 2`() {
    val prisonNumber = "S1243TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withCompletedReferral()
      requireNotNull(it.referral)
        .withSaferCustodyScreeningOutcome(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"))
        .withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "NFA"))
    }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.NO_FURTHER_ACTION)
  }

  @Test
  fun `csip status - ReferralSubmitted`() {
    val prisonNumber = "S1244TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withCompletedReferral() }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.REFERRAL_SUBMITTED)
  }

  @Test
  fun `csip status - ReferralPending`() {
    val prisonNumber = "S1245TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral() }

    val saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.REFERRAL_PENDING)
  }

  @Test
  fun `test updates update status`() {
    val prisonNumber = "S1246TS"
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral() }

    var saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.REFERRAL_PENDING)

    record.setReferralComplete()
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.REFERRAL_SUBMITTED)

    record.addSaferCustodyScreeningOutcome()
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.INVESTIGATION_PENDING)

    record.addDecisionWithoutConclusion()
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.AWAITING_DECISION)

    record.changeDecision("ACC")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.ACCT_SUPPORT)

    record.changeDecision("WIN")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.SUPPORT_OUTSIDE_CSIP)

    record.changeDecision("NFA")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.NO_FURTHER_ACTION)

    record.changeScreeningOutcome("CUR")
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.PLAN_PENDING)

    record.addPlan()
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.CSIP_OPEN)

    record.addReview(ReviewAction.CSIP_UPDATED, ReviewAction.CASE_NOTE, ReviewAction.RESPONSIBLE_PEOPLE_INFORMED)
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.CSIP_OPEN)

    record.addReview(ReviewAction.CASE_NOTE, ReviewAction.CLOSE_CSIP)
    saved = service.retrieveCsipRecord(record.id)
    assertThat(saved.status).isEqualTo(CsipStatus.CSIP_CLOSED)
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
      outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"),
    )
  }

  private fun CsipRecord.addDecisionWithoutConclusion() = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(id)
    requireNotNull(csip.referral).withDecisionAndActions(conclusion = null)
  }

  private fun CsipRecord.changeDecision(code: String) = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(id)
    requireNotNull(csip.referral!!.decisionAndActions).apply {
      set(::conclusion, "A simple conclusion")
      set(::outcome, givenReferenceData(ReferenceDataType.OUTCOME_TYPE, code))
    }
  }

  private fun CsipRecord.changeScreeningOutcome(code: String) = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(id)
    requireNotNull(csip.referral!!.saferCustodyScreeningOutcome).apply {
      set(::outcome, givenReferenceData(ReferenceDataType.OUTCOME_TYPE, code))
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
