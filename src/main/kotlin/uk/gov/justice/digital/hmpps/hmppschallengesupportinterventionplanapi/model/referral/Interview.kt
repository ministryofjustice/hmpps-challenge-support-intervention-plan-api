package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
import java.time.LocalDate
import java.util.UUID

@Schema(description = "An interview in relation to the investigation on the incident that motivated the CSIP referral")
data class Interview(
  @Schema(
    description = "The unique identifier assigned to the Interview",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val interviewUuid: UUID,

  @Schema(description = "Name of the person being interviewed.")
  val interviewee: String,

  @Schema(description = "The date the interview took place.", example = "2021-09-27")
  val interviewDate: LocalDate,

  @Schema(description = "What role the interviewee played in the incident or referral.")
  val intervieweeRole: ReferenceData,

  @Schema(description = "Information provided in interview.")
  val interviewText: String?,
)
