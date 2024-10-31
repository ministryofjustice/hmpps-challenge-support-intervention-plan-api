package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "An Attendee or Contributor to a Review of a CSIP Plan")
data class Attendee(
  @Schema(
    description = "The unique identifier assigned to the Attendee",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val attendeeUuid: UUID,

  @Schema(description = "Name of review attendee/contributor.")
  val name: String?,

  @Schema(description = "Role of review attendee/contributor.")
  val role: String?,

  @Schema(description = "If the person attended the review.")
  val isAttended: Boolean?,

  @Schema(description = "Description of attendee contribution.")
  val contribution: String?,
)
