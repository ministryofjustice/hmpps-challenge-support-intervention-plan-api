package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ReferralRequest

interface CsipRequest {
  @get:Schema(description = "User entered identifier for the CSIP record. Defaults to the prison code.")
  @get:Size(max = 10, message = "Log code must be <= 10 characters")
  val logCode: String?

  @get:Schema(description = "The referral that results in the creation of this CSIP record.")
  @get:Valid
  val referral: ReferralRequest?
}
