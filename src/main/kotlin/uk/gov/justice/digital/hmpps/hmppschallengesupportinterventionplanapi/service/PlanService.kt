package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingPlanException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreatePlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertPlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.IdentifiedNeedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getIdentifiedNeed
import java.util.UUID

@Service
@Transactional
class PlanService(
  private val csipRecordRepository: CsipRecordRepository,
  private val identifiedNeedRepository: IdentifiedNeedRepository,
) {
  fun updatePlan(recordUuid: UUID, request: UpsertPlanRequest): Plan {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = verifyExists(record.plan) { MissingPlanException(recordUuid) }
    return plan.update(request).toModel()
  }

  fun addIdentifiedNeed(recordUuid: UUID, request: CreateIdentifiedNeedRequest): IdentifiedNeed {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = verifyExists(record.plan) { MissingPlanException(recordUuid) }
    val need = plan.addIdentifiedNeed(request)
    return need.toModel()
  }

  fun updateIdentifiedNeed(identifiedNeedUuid: UUID, request: UpdateIdentifiedNeedRequest): IdentifiedNeed =
    identifiedNeedRepository.getIdentifiedNeed(identifiedNeedUuid).update(request).toModel()

  fun createPlanWithIdentifiedNeeds(recordUuid: UUID, request: CreatePlanRequest): Plan {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = record.createPlan(request)
    request.identifiedNeeds.forEach { plan.addIdentifiedNeed(it) }
    return plan.toModel()
  }
}
