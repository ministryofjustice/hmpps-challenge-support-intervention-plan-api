package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpdateReferralRequest

@Schema(description = "The request body for creating a new CSIP Record for a person")
data class CreateCsipRecordRequest(
  override val logCode: String?,
  override val referral: CreateReferralRequest,
) : CsipRequest

@Schema(description = "The request body for updating a CSIP Record")
data class UpdateCsipRecordRequest(
  override val logCode: String?,
  override val referral: UpdateReferralRequest?,
) : CsipRequest
