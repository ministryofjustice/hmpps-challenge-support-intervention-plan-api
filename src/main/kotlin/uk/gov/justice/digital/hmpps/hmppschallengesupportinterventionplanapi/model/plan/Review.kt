package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "A regular review of a CSIP Plan")
data class Review(
  @Schema(
    description = "The unique identifier assigned to the Review",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val reviewUuid: UUID,

  @Schema(
    description = "The sequence number of the Review. Used for ordering reviews correctly in lists and/or drop downs.",
    example = "3",
  )
  val reviewSequence: Int,

  @Schema(description = "The date of the review.", example = "2021-09-27")
  val reviewDate: LocalDate?,

  @Schema(description = "The username of the person who recorded the review.")
  val recordedBy: String,

  @Schema(description = "The displayable name of the person who recorded the review.")
  val recordedByDisplayName: String,

  @Schema(description = "The date of the next review.", example = "2021-09-27")
  val nextReviewDate: LocalDate?,

  @Schema(description = "The date the CSIP plan was closed following a review outcome decision to close it.")
  val csipClosedDate: LocalDate?,

  @Schema(description = "Additional information about the review.")
  val summary: String?,

  @Schema(description = "A list of actions following the review.")
  val actions: Set<ReviewAction>,

  @Schema(description = "The date and time the Review was created", example = "2021-09-27T14:19:25")
  val createdAt: LocalDateTime,

  @Schema(description = "The username of the user who created the Review", example = "USER1234")
  val createdBy: String,

  @Schema(description = "The date and time the Review was last modified", example = "2022-07-15T15:24:56")
  val lastModifiedAt: LocalDateTime?,

  @Schema(description = "The username of the user who last modified the Review", example = "USER1234")
  val lastModifiedBy: String?,

  @Schema(description = "The attendees/contributors to the review.")
  val attendees: Collection<Attendee>,
)
