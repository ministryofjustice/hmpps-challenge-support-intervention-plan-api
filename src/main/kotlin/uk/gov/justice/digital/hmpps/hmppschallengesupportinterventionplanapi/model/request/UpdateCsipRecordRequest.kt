package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(
  description = "The request body for updating a CSIP Record",
)
data class UpdateCsipRecordRequest(
  @Schema(
    description = "User entered identifier for the CSIP record. Defaults to the prison code.",
  )
  @field:Size(max = 10, message = "Log number must be <= 10 characters")
  val logNumber: String?,
)
