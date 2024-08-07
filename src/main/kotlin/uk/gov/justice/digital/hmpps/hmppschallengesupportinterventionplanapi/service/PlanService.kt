package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingPlanException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreatePlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertPlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import java.util.UUID

@Service
@Transactional
class PlanService(
  private val csipRecordRepository: CsipRecordRepository,
) {
  fun upsertPlan(recordUuid: UUID, request: UpsertPlanRequest): Plan {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = record.plan
    return csipRecordRepository.save(record.upsertPlan(csipRequestContext(), request)).plan!!.toModel()
      .apply { new = plan == null }
  }

  fun addIdentifiedNeed(recordUuid: UUID, request: CreateIdentifiedNeedRequest): IdentifiedNeed {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = verifyExists(record.plan) { MissingPlanException(recordUuid) }
    val need = plan.addIdentifiedNeed(csipRequestContext(), request)
    csipRecordRepository.save(record)
    return need.toModel()
  }

  fun createPlanWithIdentifiedNeeds(recordUuid: UUID, request: CreatePlanRequest): Plan {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    verifyDoesNotExist(record.plan) { ResourceAlreadyExistException("CSIP record already has a plan") }
    val context = csipRequestContext()
    val plan = record.upsertPlan(context, request).plan!!
    request.identifiedNeeds.forEach { plan.addIdentifiedNeed(context, it) }
    return csipRecordRepository.save(record).plan!!.toModel()
  }
}

private fun uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.IdentifiedNeed.toModel(): IdentifiedNeed =
  IdentifiedNeed(
    identifiedNeedUuid,
    identifiedNeed,
    responsiblePerson,
    createdDate,
    targetDate,
    closedDate,
    intervention,
    progression,
    createdAt,
    createdBy,
    createdByDisplayName,
    lastModifiedAt,
    lastModifiedBy,
    lastModifiedByDisplayName,
  )

private fun uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan.toModel(): Plan =
  Plan(
    caseManager,
    reasonForPlan,
    firstCaseReviewDate,
    identifiedNeeds().map { it.toModel() },
    reviews().map { it.toModel() },
  )
