package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "The request body to create a Attendee/Contributor to the review of a CSIP Plan")
data class CreateAttendeeRequest(
  @Schema(description = "Name of review attendee/contributor.")
  @field:Size(min = 0, max = 100, message = "Attendee name must be <= 100 characters")
  override val name: String?,

  @Schema(description = "Role of review attendee/contributor.")
  @field:Size(min = 0, max = 50, message = "Attendee Role must be <= 50 characters")
  override val role: String?,

  @Schema(description = "If the person attended the review.")
  override val isAttended: Boolean?,

  @Schema(description = "Description of attendee contribution.")
  @field:Size(min = 0, max = 4000, message = "Contribution must be <= 4000 characters")
  override val contribution: String?,
) : AttendeeRequest
