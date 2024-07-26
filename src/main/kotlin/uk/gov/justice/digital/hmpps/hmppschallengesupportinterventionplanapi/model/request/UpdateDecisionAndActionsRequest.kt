package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import java.time.LocalDate

@Schema(
  description = "The request body to update a Decision and Actions for a CSIP referral",
)
data class UpdateDecisionAndActionsRequest(
  @Schema(
    description = "The conclusion of the referral and reasons for the outcome decision.",
  )
  @field:Size(min = 0, max = 4000, message = "Conclusion must be <= 4000 characters")
  val conclusion: String?,

  @Schema(
    description = "The outcome decision for the referral.",
  )
  @field:Size(min = 1, max = 12, message = "Outcome Type code must be <= 12 characters")
  val outcomeTypeCode: String,

  @Schema(
    description = "The role of the person making the outcome decision.",
  )
  @field:Size(min = 1, max = 12, message = "Outcome Sign Off By Role code must be <= 12 characters")
  val signedOffByRoleCode: String?,

  @Schema(
    description = "The username of the user who recorded the outcome decision.",
  )
  @field:Size(min = 0, max = 100, message = "Outcome Recorded By username must be <= 100 characters")
  val recordedBy: String?,

  @Schema(
    description = "The displayable name of the user who recorded the outcome decision.",
  )
  @field:Size(min = 0, max = 255, message = "Outcome Recorded By display name must be <= 255 characters")
  val recordedByDisplayName: String?,

  @Schema(
    description = "The date the outcome decision was made.",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val date: LocalDate?,

  @Schema(
    description = "The next steps that should be taken following the outcome decision.",
  )
  @field:Size(min = 0, max = 4000, message = "Next Step must be <= 4000 characters")
  val nextSteps: String?,

  @Schema(
    description = "Any other actions that are recommended to be considered.",
  )
  @field:Size(min = 0, max = 4000, message = "Action Other must be <= 4000 characters")
  val actionOther: String?,

  @Schema(
    description = "A list of recommended actions.",
  )
  val actions: Set<DecisionAction> = setOf(),
)
