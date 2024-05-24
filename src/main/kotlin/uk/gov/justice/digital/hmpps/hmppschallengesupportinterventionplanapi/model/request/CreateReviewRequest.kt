package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(
  description = "The request body to create a Review for a CSIP Plan",
)
data class CreateReviewRequest(
  @Schema(
    description = "The date of the review.",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val reviewDate: LocalDate?,

  @Schema(
    description = "The username of the person who recorded the review.",
  )
  @field:Size(min = 0, max = 32, message = "Recorded By username must be <= 32 characters")
  val recordedBy: String,

  @Schema(
    description = "The displayable name of the person who recorded the review.",
  )
  @field:Size(min = 0, max = 255, message = "Recorded By display name must be <= 255 characters")
  val recordedByDisplayName: String,

  @Schema(
    description = "The date of the next review.",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val nextReviewDate: LocalDate?,

  @Schema(
    description = "If an action following the review was to inform people responsible for the person in prison.",
  )
  val isActionResponsiblePeopleInformed: Boolean?,

  @Schema(
    description = "If an action following the review was to update the CSIP plan.",
  )
  val isActionCsipUpdated: Boolean?,

  @Schema(
    description = "If the outcome decision following the review was the person should remain on the CSIP plan.",
  )
  val isActionRemainOnCsip: Boolean?,

  @Schema(
    description = "If an action following the review was to add a CSIP case note.",
  )
  val isActionCaseNote: Boolean?,

  @Schema(
    description = "If the outcome decision following the review was closing the CSIP plan.",
  )
  val isActionCloseCsip: Boolean?,

  @Schema(
    description = "The date the CSIP plan was closed following a review outcome decision to close it.",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val csipClosedDate: LocalDate?,

  @Schema(
    description = "Additional information about the review.",
  )
  @field:Size(min = 0, max = 4000, message = "Summary must be <= 4000 characters")
  val summary: String?,

  @Schema(
    description = "The attendees/contributors to the review.",
  )
  @field:Valid
  val attendees: Collection<CreateAttendeeRequest>?,
)
