package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import java.time.LocalDate

@Schema(description = "The request body to create a Review for a CSIP Plan")
data class CreateReviewRequest(
  override val reviewDate: LocalDate,
  override val recordedBy: String,
  override val recordedByDisplayName: String,
  override val nextReviewDate: LocalDate?,
  override val csipClosedDate: LocalDate?,
  override val summary: String?,
  override val actions: Set<ReviewAction> = setOf(),
  override val attendees: List<CreateAttendeeRequest>,
) : ReviewRequest, AttendeesRequest

@Schema(description = "The request body to update a Review for a CSIP Plan")
data class UpdateReviewRequest(
  override val reviewDate: LocalDate,
  override val recordedBy: String,
  override val recordedByDisplayName: String,
  override val nextReviewDate: LocalDate?,
  override val csipClosedDate: LocalDate?,
  override val summary: String?,
  override val actions: Set<ReviewAction>,
) : ReviewRequest
