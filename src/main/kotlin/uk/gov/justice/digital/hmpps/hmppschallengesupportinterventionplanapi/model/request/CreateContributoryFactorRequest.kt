package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(
  description = "The request body to create a contributory factor " +
    "to the incident that motivated the CSIP referral",
)
data class CreateContributoryFactorRequest(
  @Schema(
    description = "The type of contributory factor to the incident or motivation for CSIP referral.",
  )
  @field:Size(min = 1, max = 12, message = "Contributory factor type code must be <= 12 characters")
  val factorTypeCode: String,

  @Schema(
    description = "Additional information about the contributory factor to the incident or motivation for CSIP referral.",
  )
  val comment: String?,
)
