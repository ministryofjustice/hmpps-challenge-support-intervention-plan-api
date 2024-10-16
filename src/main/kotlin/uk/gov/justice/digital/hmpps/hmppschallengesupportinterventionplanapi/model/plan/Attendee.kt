package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
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

  @Schema(description = "The date and time the Attendee was created", example = "2021-09-27T14:19:25")
  val createdAt: LocalDateTime,

  @Schema(description = "The username of the user who created the Attendee", example = "USER1234")
  val createdBy: String,

  @Schema(description = "The displayable name of the user who created the Attendee", example = "Firstname Lastname")
  val createdByDisplayName: String,

  @Schema(description = "The date and time the Attendee was last modified", example = "2022-07-15T15:24:56")
  val lastModifiedAt: LocalDateTime?,

  @Schema(description = "The username of the user who last modified the Attendee", example = "USER1234")
  val lastModifiedBy: String?,

  @Schema(
    description = "The displayable name of the user who last modified the Attendee",
    example = "Firstname Lastname",
  )
  val lastModifiedByDisplayName: String?,
)
