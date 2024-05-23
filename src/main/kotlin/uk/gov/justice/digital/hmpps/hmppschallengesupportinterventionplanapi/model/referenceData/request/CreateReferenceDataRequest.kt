package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(
  description = "The request body to create a new code to a reference data table",
)
data class CreateReferenceDataRequest(
  @Schema(
    description = "The short code for a reference data",
  )
  @field:Size(min = 1, max = 12, message = "Reference data code must be <= 12 characters")
  val code: String,

  @Schema(
    description = "The description for the reference data code",
  )
  @field:Size(min = 0, max = 40, message = "Reference data description must be <= 40 characters")
  val description: String?,
)
