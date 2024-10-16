package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
import java.time.LocalDate

@Schema(description = "The Safer Custody Screening Outcome to the CSIP referral")
data class SaferCustodyScreeningOutcome(
  @Schema(description = "The type of outcome of the safer custody screening.")
  val outcome: ReferenceData,

  @Schema(description = "The username of the user who recorded the safer custody screening outcome.")
  val recordedBy: String,

  @Schema(description = "The displayable name of the user who recorded the safer custody screening outcome.")
  val recordedByDisplayName: String,

  @Schema(description = "The date of the safer custody screening outcome.", example = "2021-09-27")
  val date: LocalDate,

  @Schema(description = "The reasons for the safer custody screening outcome decision.")
  val reasonForDecision: String?,
)
