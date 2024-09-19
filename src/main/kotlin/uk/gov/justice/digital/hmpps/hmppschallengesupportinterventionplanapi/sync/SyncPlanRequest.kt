package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ValidPlanDetail
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.AttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.IdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.IdentifiedNeedsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.PlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ReviewsRequest
import java.time.LocalDate
import java.util.UUID

@ValidPlanDetail
data class SyncPlanRequest(
  @field:Size(min = 0, max = 100, message = "Case manager must be <= 100 characters")
  override val caseManager: String?,
  @field:Size(min = 0, max = 240, message = "Reason for plan must be <= 240 characters")
  override val reasonForPlan: String?,
  override val firstCaseReviewDate: LocalDate?,
  @field:Valid
  override val identifiedNeeds: List<SyncNeedRequest>,
  @field:Valid
  override val reviews: List<SyncReviewRequest>,
) : PlanRequest, IdentifiedNeedsRequest, ReviewsRequest {
  fun requestMappings(): Set<RequestMapping> = buildSet {
    addAll(identifiedNeeds.map { RequestMapping(CsipComponent.IDENTIFIED_NEED, it.legacyId, it.id) })
    addAll(reviews.flatMap(SyncReviewRequest::requestMappings))
  }
}

data class SyncNeedRequest(
  @field:Size(min = 0, max = 1000, message = "Identified need must be <= 1000 characters")
  override val identifiedNeed: String,
  @field:Size(min = 0, max = 100, message = "Responsible person name must be <= 100 characters")
  override val responsiblePerson: String,
  override val createdDate: LocalDate,
  override val targetDate: LocalDate,
  override val closedDate: LocalDate?,
  @field:Size(min = 0, max = 4000, message = "Intervention must be <= 4000 characters")
  override val intervention: String,
  @field:Size(min = 0, max = 4000, message = "Progression must be <= 4000 characters")
  override val progression: String?,
  override val legacyId: Long,
  override val id: UUID?,
) : NomisAudited(), NomisIdentifiable, IdentifiedNeedRequest, LegacyIdAware

data class SyncReviewRequest(
  override val reviewDate: LocalDate?,
  @field:Size(min = 0, max = 64, message = "Recorded by username must be <= 64 characters")
  override val recordedBy: String,
  @field:Size(min = 0, max = 255, message = "Recorded by display name must be <= 255 characters")
  override val recordedByDisplayName: String,
  override val nextReviewDate: LocalDate?,
  override val csipClosedDate: LocalDate?,
  @field:Size(min = 0, max = 4000, message = "Summary must be <= 4000 characters")
  override val summary: String?,
  override val actions: Set<ReviewAction>,
  @field:Valid
  val attendees: List<SyncAttendeeRequest>,
  override val legacyId: Long,
  override val id: UUID?,
) : NomisAudited(), NomisIdentifiable, ReviewRequest, LegacyIdAware {
  fun requestMappings(): Set<RequestMapping> = buildSet {
    add(RequestMapping(CsipComponent.REVIEW, legacyId, id))
    addAll(attendees.map { RequestMapping(CsipComponent.ATTENDEE, it.legacyId, it.id) })
  }
}

data class SyncAttendeeRequest(
  @field:Size(min = 0, max = 100, message = "Attendee name must be <= 100 characters")
  override val name: String?,
  @field:Size(min = 0, max = 50, message = "Attendee role must be <= 50 characters")
  override val role: String?,
  override val isAttended: Boolean?,
  @field:Size(min = 0, max = 4000, message = "Contribution must be <= 4000 characters")
  override val contribution: String?,
  override val legacyId: Long,
  override val id: UUID?,
) : NomisAudited(), NomisIdentifiable, AttendeeRequest, LegacyIdAware
