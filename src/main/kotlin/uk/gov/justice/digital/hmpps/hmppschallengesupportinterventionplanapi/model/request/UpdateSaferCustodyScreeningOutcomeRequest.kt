package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "The request body to update the Safer Custody Screening Outcome to the CSIP referral")
data class UpdateSaferCustodyScreeningOutcomeRequest(
  @Schema(
    description = "The type of outcome of the safer custody screening.",
  )
  @field:Size(min = 1, max = 12, message = "Outcome Type code must be <= 12 characters")
  val outcomeTypeCode: String,

  @Schema(
    description = "The username of the user who recorded the safer custody screening outcome.",
  )
  @field:Size(min = 0, max = 100, message = "Recorder username must be <= 100 characters")
  val recordBy: String,

  @Schema(
    description = "The displayable name of the user who recorded the safer custody screening outcome.",
  )
  @field:Size(min = 0, max = 255, message = "Recorder display name must be <= 255 characters")
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
  @field:Size(min = 0, max = 4000, message = "Reason for Decision must be <= 4000 characters")
  val reasonForDecision: String,
)
