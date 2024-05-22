package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(
  description = "The request body to update the description of a code in a reference data table",
)
data class UpdateReferenceDataRequest(
  @Schema(
    description = "The description for the reference data code",
  )
  @field:Size(min = 0, max = 40, message = "Reference data description must be <= 40 characters")
  val description: String?,
)
