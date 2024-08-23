package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import java.time.LocalDate

@Schema(
  description = "The request body to create a Review for a CSIP Plan",
)
data class CreateReviewRequest(
  @Schema(description = "The date of the review.", example = "2021-09-27")
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val reviewDate: LocalDate?,

  @Schema(description = "The username of the person who recorded the review.")
  @field:Size(min = 0, max = 64, message = "Recorded By username must be <= 64 characters")
  override val recordedBy: String,

  @Schema(description = "The displayable name of the person who recorded the review.")
  @field:Size(min = 0, max = 255, message = "Recorded By display name must be <= 255 characters")
  override val recordedByDisplayName: String,

  @Schema(description = "The date of the next review.", example = "2021-09-27")
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val nextReviewDate: LocalDate?,

  @Schema(description = "The date the CSIP plan was closed following a review outcome decision to close it.", example = "2021-09-27")
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val csipClosedDate: LocalDate?,

  @Schema(description = "Additional information about the review.")
  @field:Size(min = 0, max = 4000, message = "Summary must be <= 4000 characters")
  override val summary: String?,

  @Schema(description = "A list of actions following the review.")
  override val actions: Set<ReviewAction> = setOf(),

  @Schema(description = "The attendees/contributors to the review.")
  @field:Valid
  override val attendees: List<CreateAttendeeRequest>,
) : ReviewRequest, AttendeesRequest
