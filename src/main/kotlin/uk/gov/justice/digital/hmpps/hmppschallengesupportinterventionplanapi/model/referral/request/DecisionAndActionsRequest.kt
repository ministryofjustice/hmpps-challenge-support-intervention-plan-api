package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import java.time.LocalDate

interface DecisionAndActionsRequest {
  @get:Schema(description = "The conclusion of the referral and reasons for the outcome decision.")
  @get:Size(min = 0, max = 4000, message = "Conclusion must be <= 4000 characters")
  val conclusion: String?

  @get:Schema(description = "The outcome decision for the referral.")
  @get:Size(min = 1, max = 12, message = "Decision outcome code must be <= 12 characters")
  val outcomeTypeCode: String?

  @get:Schema(description = "The role of the person making the outcome decision.")
  @get:Size(min = 0, max = 12, message = "Signed off by role code must be <= 12 characters")
  val signedOffByRoleCode: String?

  @get:Schema(description = "The username of the user who recorded the outcome decision.")
  @get:Size(min = 0, max = 64, message = "Recorded by username must be <= 64 characters")
  val recordedBy: String?

  @get:Schema(description = "The displayable name of the user who recorded the outcome decision.")
  @get:Size(min = 0, max = 255, message = "Recorded by display name must be <= 255 characters")
  val recordedByDisplayName: String?

  @get:Schema(description = "The date the outcome decision was made.", example = "2021-09-27")
  val date: LocalDate?

  @get:Schema(description = "The next steps that should be taken following the outcome decision.")
  @get:Size(min = 0, max = 4000, message = "Next steps must be <= 4000 characters")
  val nextSteps: String?

  @get:Schema(description = "Any other actions that are recommended to be considered.")
  @get:Size(min = 0, max = 4000, message = "Action other must be <= 4000 characters")
  val actionOther: String?

  @get:Schema(description = "A list of recommended actions.")
  val actions: Set<DecisionAction>
}

@Schema(description = "The request body to create a Decision and Actions for a CSIP referral")
data class UpsertDecisionAndActionsRequest(
  override val conclusion: String?,
  override val outcomeTypeCode: String,
  override val signedOffByRoleCode: String?,
  override val recordedBy: String?,
  override val recordedByDisplayName: String?,
  override val date: LocalDate?,
  override val nextSteps: String?,
  override val actionOther: String?,
  override val actions: Set<DecisionAction> = setOf(),
) : DecisionAndActionsRequest
