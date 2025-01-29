package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.IdentifiedNeedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.getIdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PublishCsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingPlanException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreatePlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.UpdateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.UpdatePlanRequest
import java.util.UUID

@Service
@Transactional
class PlanService(
  private val csipRecordRepository: CsipRecordRepository,
  private val identifiedNeedRepository: IdentifiedNeedRepository,
) {
  @PublishCsipEvent(CSIP_UPDATED)
  fun updatePlan(recordUuid: UUID, request: UpdatePlanRequest): Plan {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = verifyExists(record.plan) { MissingPlanException(recordUuid) }
    return plan.update(request).toModel()
  }

  @PublishCsipEvent(CSIP_UPDATED)
  fun addIdentifiedNeed(recordUuid: UUID, request: CreateIdentifiedNeedRequest): IdentifiedNeed {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = verifyExists(record.plan) { MissingPlanException(recordUuid) }
    val need = plan.addIdentifiedNeed(request)
    return need.toModel()
  }

  @PublishCsipEvent(CSIP_UPDATED)
  fun updateIdentifiedNeed(identifiedNeedUuid: UUID, request: UpdateIdentifiedNeedRequest): IdentifiedNeed = identifiedNeedRepository.getIdentifiedNeed(identifiedNeedUuid).update(request).toModel()

  @PublishCsipEvent(CSIP_UPDATED)
  fun createPlanWithIdentifiedNeeds(recordUuid: UUID, request: CreatePlanRequest): Plan {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = record.createPlan(request)
    request.identifiedNeeds.forEach { plan.addIdentifiedNeed(it) }
    return plan.toModel()
  }
}
