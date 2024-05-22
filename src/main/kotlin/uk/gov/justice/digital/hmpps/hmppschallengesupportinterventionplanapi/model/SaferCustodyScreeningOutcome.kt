package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.OutcomeType
import java.time.LocalDate

@Schema(description = "The Safer Custody Screening Outcome to the CSIP referral")
data class SaferCustodyScreeningOutcome(
  @Schema(
    description = "The type of outcome of the safer custody screening.",
  )
  val outcome: OutcomeType,

  @Schema(
    description = "The username of the user who recorded the safer custody screening outcome.",
  )
  val recordBy: String,

  @Schema(
    description = "The displayable name of the user who recorded the safer custody screening outcome.",
  )
  val recordByDisplayName: String,

  @Schema(
    description = "The date of the safer custody screening outcome.",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val date: LocalDate,

  @Schema(
    description = "The reasons for the safer custody screening outcome decision.",
  )
  val reasonForDecision: String,
)
