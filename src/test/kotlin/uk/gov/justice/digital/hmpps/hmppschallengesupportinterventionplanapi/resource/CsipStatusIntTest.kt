package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
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

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @Test
  fun `csip status - CsipClosed`() {
    val prisonNumber = "S1234TS"
    val record = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), true)
        .withPlan()
      val plan = requireNotNull(csip.plan)
      plan.withReview(actions = setOf(ReviewAction.CsipUpdated))
        .withReview(actions = setOf(ReviewAction.CloseCsip))
      csip
    }!!

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.CSIP_CLOSED)
  }

  @Test
  fun `csip status - CsipOpen`() {
    val prisonNumber = "S1235TS"
    val record = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), true)
        .withPlan()
      val plan = requireNotNull(csip.plan)
      plan.withReview(actions = setOf(ReviewAction.CsipUpdated))
        .withReview(actions = setOf(ReviewAction.RemainOnCsip))
      csip
    }!!

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.CSIP_OPEN)
  }

  @Test
  fun `csip status - AwaitingDecision`() {
    val prisonNumber = "S1236TS"
    val record = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), complete = true)
      requireNotNull(csip.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"),
      ).withDecisionAndActions(conclusion = null)
      csip
    }!!

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.AWAITING_DECISION)
  }

  @Test
  fun `csip status - AcctSupport 1`() {
    val prisonNumber = "S1237TS"
    val record = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), complete = true)
      requireNotNull(csip.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "ACC"),
      )
      csip
    }!!

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.ACCT_SUPPORT)
  }

  @Test
  fun `csip status - AcctSupport 2`() {
    val prisonNumber = "S1238TS"
    val record = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), complete = true)
      requireNotNull(csip.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"),
      ).withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "ACC"))
      csip
    }!!

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.ACCT_SUPPORT)
  }

  @Test
  fun `csip status - PlanPending 1`() {
    val prisonNumber = "S1239TS"
    val record = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), complete = true)
      requireNotNull(csip.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "CUR"),
      )
      csip
    }!!

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.PLAN_PENDING)
  }

  @Test
  fun `csip status - PlanPending 2`() {
    val prisonNumber = "S1240TS"
    val record = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), complete = true)
      requireNotNull(csip.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"),
      ).withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "CUR"))
      csip
    }!!

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.PLAN_PENDING)
  }

  @Test
  fun `csip status - InvestigationPending`() {
    val prisonNumber = "S1241TS"
    val record = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), complete = true)
      requireNotNull(csip.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"),
      )
      csip
    }!!

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.INVESTIGATION_PENDING)
  }

  @Test
  fun `csip status - NoFurtherAction 1`() {
    val prisonNumber = "S1242TS"
    val record = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), complete = true)
      requireNotNull(csip.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "NFA"),
      )
      csip
    }!!

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.NO_FURTHER_ACTION)
  }

  @Test
  fun `csip status - NoFurtherAction 2`() {
    val prisonNumber = "S1243TS"
    val record = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), complete = true)
      requireNotNull(csip.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"),
      ).withDecisionAndActions(outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "NFA"))
      csip
    }!!

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.NO_FURTHER_ACTION)
  }

  @Test
  fun `csip status - ReferralSubmitted`() {
    val prisonNumber = "S1244TS"
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), complete = true)

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.REFERRAL_SUBMITTED)
  }

  @Test
  fun `csip status - ReferralPending`() {
    val prisonNumber = "S1245TS"
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))

    val saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.REFERRAL_PENDING)
  }

  @Test
  fun `test updates update status`() {
    val prisonNumber = "S1246TS"
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))

    var saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.REFERRAL_PENDING)

    record.setReferralComplete()
    saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.REFERRAL_SUBMITTED)

    record.addSaferCustodyScreeningOutcome()
    saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.INVESTIGATION_PENDING)

    record.addDecisionWithoutConclusion()
    saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.AWAITING_DECISION)

    record.changeDecision("ACC")
    saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.ACCT_SUPPORT)

    record.changeDecision("WIN")
    saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.SUPPORT_OUTSIDE_CSIP)

    record.changeDecision("NFA")
    saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.NO_FURTHER_ACTION)

    record.changeScreeningOutcome("CUR")
    saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.PLAN_PENDING)

    record.addPlan()
    saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.CSIP_OPEN)

    record.addReview(ReviewAction.CsipUpdated, ReviewAction.CaseNote, ReviewAction.ResponsiblePeopleInformed)
    saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.CSIP_OPEN)

    record.addReview(ReviewAction.CaseNote, ReviewAction.CloseCsip)
    saved = service.retrieveCsipRecord(record.recordUuid)
    assertThat(saved.status).isEqualTo(CsipStatus.CSIP_CLOSED)
  }

  private fun CsipRecord.setReferralComplete() = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(recordUuid)
    requireNotNull(csip.referral).apply {
      set(::referralComplete, true)
      set(::referralCompletedDate, LocalDate.now())
      set(::referralCompletedBy, "referralCompletedBy")
      set(::referralCompletedByDisplayName, "referralCompletedByDisplayName")
    }
    csipRecordRepository.save(csip)
  }

  private fun CsipRecord.addSaferCustodyScreeningOutcome() = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(recordUuid)
    requireNotNull(csip.referral).withSaferCustodyScreeningOutcome(
      outcome = givenReferenceData(ReferenceDataType.OUTCOME_TYPE, "OPE"),
    )
    csipRecordRepository.save(csip)
  }

  private fun CsipRecord.addDecisionWithoutConclusion() = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(recordUuid)
    requireNotNull(csip.referral).withDecisionAndActions(conclusion = null)
    csipRecordRepository.save(csip)
  }

  private fun CsipRecord.changeDecision(code: String) = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(recordUuid)
    requireNotNull(csip.referral!!.decisionAndActions).apply {
      set(::conclusion, "A simple conclusion")
      set(::outcome, givenReferenceData(ReferenceDataType.OUTCOME_TYPE, code))
    }
    csipRecordRepository.save(csip)
  }

  private fun CsipRecord.changeScreeningOutcome(code: String) = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(recordUuid)
    requireNotNull(csip.referral!!.saferCustodyScreeningOutcome).apply {
      set(::outcome, givenReferenceData(ReferenceDataType.OUTCOME_TYPE, code))
    }
    csipRecordRepository.save(csip)
  }

  private fun CsipRecord.addPlan() = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(recordUuid).withPlan()
    csipRecordRepository.save(csip)
  }

  private fun CsipRecord.addReview(vararg actions: ReviewAction) = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(recordUuid)
    requireNotNull(csip.plan).withReview(actions = buildSet { addAll(actions) })
    csipRecordRepository.save(csip)
  }
}
