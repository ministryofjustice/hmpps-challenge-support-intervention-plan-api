package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertPlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import java.util.UUID

@Service
@Transactional
class PlanService(
  private val csipRecordRepository: CsipRecordRepository,
) {
  fun upsertPlan(
    recordUuid: UUID,
    request: UpsertPlanRequest,
  ): Plan {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = record.plan
    return csipRecordRepository.save(record.upsertPlan(csipRequestContext(), request)).plan!!.toModel()
      .apply { new = plan == null }
  }
}

fun uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan.toModel(): Plan =
  Plan(caseManager, reasonForPlan, firstCaseReviewDate, listOf(), listOf())
