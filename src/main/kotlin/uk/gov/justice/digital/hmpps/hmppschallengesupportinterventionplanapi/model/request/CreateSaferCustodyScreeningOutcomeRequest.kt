package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "The request body to create the Safer Custody Screening Outcome to the CSIP referral")
data class CreateSaferCustodyScreeningOutcomeRequest(
  @Schema(description = "The type of outcome of the safer custody screening.")
  @field:Size(min = 1, max = 12, message = "Outcome Type code must be <= 12 characters")
  override val outcomeTypeCode: String,

  @Schema(
    description = "The date of the safer custody screening outcome.",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val date: LocalDate,

  @Schema(
    description = "The reasons for the safer custody screening outcome decision.",
  )
  @field:Size(min = 0, max = 4000, message = "Reason for Decision must be <= 4000 characters")
  override val reasonForDecision: String,

  @Schema(
    description = "The username of the user who recorded the screening outcome.",
  )
  @field:Size(min = 0, max = 64, message = "Recorded by username must be <= 64 characters")
  override val recordedBy: String,

  @Schema(
    description = "The displayable name of the user who recorded the screening outcome.",
  )
  @field:Size(min = 0, max = 255, message = "Recorded by display name must be <= 255 characters")
  override val recordedByDisplayName: String,
) : ScreeningOutcomeRequest
