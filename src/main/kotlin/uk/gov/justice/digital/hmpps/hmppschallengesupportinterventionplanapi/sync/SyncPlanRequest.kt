package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.ValidPlanDetail
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.AttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.AttendeesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.FirstReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.IdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.IdentifiedNeedsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.PlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.ReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.ReviewsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDate
import java.util.UUID

@ValidPlanDetail
data class SyncPlanRequest(
  override val caseManager: String?,
  override val reasonForPlan: String?,
  override val firstCaseReviewDate: LocalDate?,
  override val identifiedNeeds: List<SyncNeedRequest>,
  override val reviews: List<SyncReviewRequest>,
) : PlanRequest, FirstReviewRequest, IdentifiedNeedsRequest, ReviewsRequest {
  override val nextCaseReviewDate: LocalDate? = firstCaseReviewDate

  fun requestMappings(): Set<RequestMapping> = buildSet {
    addAll(identifiedNeeds.map { RequestMapping(CsipComponent.IDENTIFIED_NEED, it.legacyId, it.id) })
    addAll(reviews.flatMap(SyncReviewRequest::requestMappings))
  }
}

data class SyncNeedRequest(
  override val identifiedNeed: String,
  override val responsiblePerson: String,
  override val createdDate: LocalDate,
  override val targetDate: LocalDate,
  override val closedDate: LocalDate?,
  override val intervention: String,
  override val progression: String?,
  override val legacyId: Long,
  override val id: UUID?,
) : NomisIdentifiable, IdentifiedNeedRequest, LegacyIdAware

data class SyncReviewRequest(
  override val reviewDate: LocalDate,
  override val recordedBy: String,
  override val recordedByDisplayName: String,
  override val nextReviewDate: LocalDate?,
  override val csipClosedDate: LocalDate?,
  override val summary: String?,
  override val actions: Set<ReviewAction>,
  override val attendees: List<SyncAttendeeRequest>,
  override val legacyId: Long,
  override val id: UUID?,
) : NomisIdentifiable, ReviewRequest, AttendeesRequest, LegacyIdAware {
  fun requestMappings(): Set<RequestMapping> = buildSet {
    add(RequestMapping(CsipComponent.REVIEW, legacyId, id))
    addAll(attendees.map { RequestMapping(CsipComponent.ATTENDEE, it.legacyId, it.id) })
  }
}

data class SyncAttendeeRequest(
  override val name: String?,
  override val role: String?,
  override val isAttended: Boolean?,
  override val contribution: String?,
  override val legacyId: Long,
  override val id: UUID?,
) : NomisIdentifiable, AttendeeRequest, LegacyIdAware
