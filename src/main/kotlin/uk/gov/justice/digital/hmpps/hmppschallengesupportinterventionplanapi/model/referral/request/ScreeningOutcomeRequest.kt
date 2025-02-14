package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

interface ScreeningOutcomeRequest {
  @get:Schema(description = "The type of outcome of the safer custody screening.")
  @get:Size(min = 1, max = 12, message = "Screening outcome code must be <= 12 characters")
  val outcomeTypeCode: String

  @get:Schema(description = "The reasons for the safer custody screening outcome decision.")
  @get:Size(min = 0, max = 4000, message = "Reason for decision must be <= 4000 characters")
  val reasonForDecision: String?

  @get:Schema(description = "The date of the safer custody screening outcome.", example = "2021-09-27")
  val date: LocalDate

  @get:Schema(description = "The username of the user who recorded the screening outcome.")
  @get:Size(min = 0, max = 64, message = "Recorded by username must be <= 64 characters")
  val recordedBy: String

  @get:Schema(description = "The displayable name of the user who recorded the screening outcome.")
  @get:Size(min = 0, max = 255, message = "Recorded by display name must be <= 255 characters")
  val recordedByDisplayName: String
}

@Schema(description = "The request body to create the Safer Custody Screening Outcome to the CSIP referral")
data class CreateSaferCustodyScreeningOutcomeRequest(
  override val outcomeTypeCode: String,
  override val date: LocalDate,
  override val reasonForDecision: String,
  override val recordedBy: String,
  override val recordedByDisplayName: String,
) : ScreeningOutcomeRequest

@Schema(description = "The request body to create or update the Safer Custody Screening Outcome on the CSIP referral")
data class UpsertSaferCustodyScreeningOutcomeRequest(
  override val outcomeTypeCode: String,
  override val date: LocalDate,
  override val reasonForDecision: String,
  override val recordedBy: String,
  override val recordedByDisplayName: String,
) : ScreeningOutcomeRequest
