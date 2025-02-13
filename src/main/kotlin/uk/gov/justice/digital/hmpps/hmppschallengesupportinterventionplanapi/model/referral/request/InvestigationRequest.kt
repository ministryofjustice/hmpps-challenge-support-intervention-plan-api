package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ValidInvestigationDetail
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ValidInvestigationDetail.Companion.WITH_INTERVIEW_MESSAGE

@Schema(description = "The request body to create an investigation on the incident that motivated the CSIP referral.")
@ValidInvestigationDetail(message = WITH_INTERVIEW_MESSAGE)
data class CreateInvestigationRequest(
  override val staffInvolved: String?,
  override val evidenceSecured: String?,
  override val occurrenceReason: String?,
  override val personsUsualBehaviour: String?,
  override val personsTrigger: String?,
  override val protectiveFactors: String?,
  val recordedBy: String?,
  val recordedByDisplayName: String?,
  override val interviews: List<CreateInterviewRequest> = listOf(),
) : InvestigationRequest,
  InterviewsRequest

@Schema(description = "The request body to update an investigation on the incident that motivated the CSIP referral.")
@ValidInvestigationDetail
data class UpdateInvestigationRequest(
  override val staffInvolved: String?,
  override val evidenceSecured: String?,
  override val occurrenceReason: String?,
  override val personsUsualBehaviour: String?,
  override val personsTrigger: String?,
  override val protectiveFactors: String?,
) : InvestigationRequest
