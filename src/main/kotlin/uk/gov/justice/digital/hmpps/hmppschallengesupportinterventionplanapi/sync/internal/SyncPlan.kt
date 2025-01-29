package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.byId
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.byLegacyId
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.ResponseMapping
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncPlanRequest
import java.util.UUID

@Service
@Transactional
class SyncPlan {
  fun sync(plan: Plan, request: SyncPlanRequest): Set<ResponseMapping> {
    val needMappings = request.identifiedNeeds.map {
      val need = plan.findNeed(it.id, it.legacyId)?.update(it) ?: plan.addIdentifiedNeed(it)
      ResponseMapping(CsipComponent.IDENTIFIED_NEED, it.legacyId, need.id)
    }.toSet()
    val reviewMappings = request.reviews.map { rev ->
      val review = plan.findReview(rev.id, rev.legacyId)?.update(rev) ?: plan.addReview(rev)
      val attendeeMappings = rev.attendees.map { att ->
        val attendee = review.findAttendee(att.id, att.legacyId)?.update(att) ?: review.addAttendee(att)
        ResponseMapping(CsipComponent.ATTENDEE, att.legacyId, attendee.id)
      }
      attendeeMappings + ResponseMapping(CsipComponent.REVIEW, rev.legacyId, review.id)
    }.toSet()
    return needMappings + reviewMappings.flatten()
  }
}

private fun Plan.findNeed(uuid: UUID?, legacyId: Long): IdentifiedNeed? = identifiedNeeds().find { it.byId(uuid) || it.byLegacyId(legacyId) }

private fun Plan.findReview(uuid: UUID?, legacyId: Long): Review? = reviews().find { it.byId(uuid) || it.byLegacyId(legacyId) }

private fun Review.findAttendee(uuid: UUID?, legacyId: Long): Attendee? = attendees().find { it.byId(uuid) || it.byLegacyId(legacyId) }
