package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData
import java.time.LocalDate

@Schema(description = "The Decision and Actions for the CSIP referral")
data class DecisionAndActions(
  @Schema(
    description = "The conclusion of the referral and reasons for the outcome decision.",
  )
  val conclusion: String?,

  @Schema(
    description = "The outcome decision for the referral.",
  )
  val outcome: ReferenceData,

  @Schema(
    description = "The role of the person making the outcome decision.",
  )
  val signedOffByRole: ReferenceData?,

  @Schema(
    description = "The username of the user who recorded the outcome decision.",
  )
  val recordedBy: String?,

  @Schema(
    description = "The displayable name of the user who recorded the outcome decision.",
  )
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
  val nextSteps: String?,

  @Schema(
    description = "A list of recommended actions.",
  )
  val actions: Set<DecisionAction>,

  @Schema(
    description = "Any other actions that are recommended to be considered.",
  )
  val actionOther: String?,
) {
  @JsonIgnore
  var new: Boolean = false
}
