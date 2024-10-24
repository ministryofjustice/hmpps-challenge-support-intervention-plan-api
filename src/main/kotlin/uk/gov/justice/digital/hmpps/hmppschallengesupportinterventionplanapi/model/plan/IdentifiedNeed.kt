package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "A need identified in the CSIP Plan")
data class IdentifiedNeed(
  @Schema(
    description = "The unique identifier assigned to the Contributory Factor",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val identifiedNeedUuid: UUID,

  @Schema(description = "Details of the need identified in the CSIP plan.")
  val identifiedNeed: String,

  @Schema(description = "The name of the person who is responsible for taking action on the intervention.")
  val responsiblePerson: String,

  @Schema(description = "The date the need was identified.", example = "2021-09-27")
  val createdDate: LocalDate,

  @Schema(description = "The target date the need should be progressed or resolved.", example = "2021-09-27")
  val targetDate: LocalDate,

  @Schema(description = "The date the identified need was resolved or closed.", example = "2021-09-27")
  val closedDate: LocalDate?,

  @Schema(description = "The planned intervention for the identified need.")
  val intervention: String,

  @Schema(description = "How the plan to address the identified need is progressing.")
  val progression: String?,

  @Schema(description = "The date and time the Identified Need was created", example = "2021-09-27T14:19:25")
  val createdAt: LocalDateTime,

  @Schema(description = "The username of the user who created the Identified Need", example = "USER1234")
  val createdBy: String,

  @Schema(
    description = "The displayable name of the user who created the Identified Need",
    example = "Firstname Lastname",
  )
  val createdByDisplayName: String,

  @Schema(description = "The date and time the Identified Need was last modified", example = "2022-07-15T15:24:56")
  val lastModifiedAt: LocalDateTime?,

  @Schema(description = "The username of the user who last modified the Identified Need", example = "USER1234")
  val lastModifiedBy: String?,

  @Schema(
    description = "The displayable name of the user who last modified the Identified Need",
    example = "Firstname Lastname",
  )
  val lastModifiedByDisplayName: String?,
)
