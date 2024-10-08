package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

@Schema(description = "The request body for creating a new CSIP Record for a person")
data class CreateCsipRecordRequest(
  @Schema(description = "User entered identifier for the CSIP record. Defaults to the prison code.")
  @field:Size(max = 10, message = "Log code must be <= 10 characters")
  val logCode: String?,

  @Schema(description = "The referral that results in the creation of this CSIP record.")
  @field:Valid
  val referral: CreateReferralRequest,
)
