package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData
import java.time.LocalDate

@Schema(description = "The Decisions and Actions for the CSIP referral")
data class DecisionsAndActions(
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
  val outcomeSignedOffByRole: ReferenceData?,

  @Schema(
    description = "The username of the user who recorded the outcome decision.",
  )
  val outcomeRecordedBy: String?,

  @Schema(
    description = "The displayable name of the user who recorded the outcome decision.",
  )
  val outcomeRecordedByDisplayName: String?,

  @Schema(
    description = "The date the outcome decision was made.",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val outcomeDate: LocalDate?,

  @Schema(
    description = "The next steps that should be taken following the outcome decision.",
  )
  val nextSteps: String?,

  @Schema(
    description = "If a recommended action is to open a CSIP alert.",
  )
  val isActionOpenCsipAlert: Boolean?,

  @Schema(
    description = "If a recommended action is to update the non associations for the person.",
  )
  val isActionNonAssociationsUpdated: Boolean?,

  @Schema(
    description = "If a recommended action is to add the person to the observation book.",
  )
  val isActionObservationBook: Boolean?,

  @Schema(
    description = "If a recommended action is to move the person.",
  )
  val isActionUnitOrCellMove: Boolean?,

  @Schema(
    description = "If a recommended action is to perform a CSRA/RSRA review.",
  )
  val isActionCsraOrRsraReview: Boolean?,

  @Schema(
    description = "If a recommended action is to refer the person to another service.",
  )
  val isActionServiceReferral: Boolean?,

  @Schema(
    description = "If a recommended action is to refer the person to SIM.",
  )
  val isActionSimReferral: Boolean?,

  @Schema(
    description = "Any other actions that are recommended to be considered.",
  )
  val actionOther: String?,
)
