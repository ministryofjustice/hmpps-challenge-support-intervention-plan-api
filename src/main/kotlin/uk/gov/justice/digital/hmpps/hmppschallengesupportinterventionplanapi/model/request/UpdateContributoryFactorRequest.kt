package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "The request body to update a contributory factor to the incident that motivated the CSIP referral")
data class UpdateContributoryFactorRequest(
  @Schema(description = "Additional information about the contributory factor to the incident or motivation for CSIP referral.")
  @field:Size(max = 4000, message = "Comment must be less than 4000 characters")
  override val comment: String?,
) : CommentRequest
