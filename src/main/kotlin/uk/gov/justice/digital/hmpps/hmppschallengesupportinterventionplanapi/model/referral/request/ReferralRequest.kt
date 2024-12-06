package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "The request body for creating a CSIP referral")
data class CreateReferralRequest(
  override val incidentDate: LocalDate,
  override val incidentTime: LocalTime?,
  override val incidentTypeCode: String,
  override val incidentLocationCode: String,
  override val referredBy: String,
  override val refererAreaCode: String,
  override val isProactiveReferral: Boolean?,
  override val isStaffAssaulted: Boolean?,
  override val assaultedStaffName: String?,
  override val incidentInvolvementCode: String?,
  override val descriptionOfConcern: String?,
  override val knownReasons: String?,
  override val otherInformation: String?,
  override val isSaferCustodyTeamInformed: OptionalYesNoAnswer,
  override val isReferralComplete: Boolean?,
  @field:NotEmpty(message = "A referral must have at least one contributory factor.")
  override val contributoryFactors: List<CreateContributoryFactorRequest>,
) : ReferralRequest, ContributoryFactorsRequest

@Schema(description = "The detail for updating a CSIP referral")
data class UpdateReferralRequest(
  override val incidentDate: LocalDate,
  override val incidentTime: LocalTime?,
  override val incidentTypeCode: String,
  override val incidentLocationCode: String,
  override val referredBy: String,
  override val refererAreaCode: String,
  override val isProactiveReferral: Boolean?,
  override val isStaffAssaulted: Boolean?,
  override val assaultedStaffName: String?,
  override val incidentInvolvementCode: String?,
  override val descriptionOfConcern: String?,
  override val knownReasons: String?,
  override val otherInformation: String?,
  override val isSaferCustodyTeamInformed: OptionalYesNoAnswer,
  override val isReferralComplete: Boolean?,
) : ReferralRequest

@Schema(description = "The detail for updating a CSIP referral")
data class MergeReferralRequest(
  override val incidentDate: LocalDate,
  override val incidentTime: LocalTime?,
  override val incidentTypeCode: String,
  override val incidentLocationCode: String,
  override val referredBy: String,
  override val refererAreaCode: String,
  override val isProactiveReferral: Boolean?,
  override val isStaffAssaulted: Boolean?,
  override val assaultedStaffName: String?,
  override val incidentInvolvementCode: String?,
  override val descriptionOfConcern: String?,
  override val knownReasons: String?,
  override val otherInformation: String?,
  override val isSaferCustodyTeamInformed: OptionalYesNoAnswer,
  override val isReferralComplete: Boolean?,
  override val contributoryFactors: List<MergeContributoryFactorRequest>,
) : ReferralRequest, ContributoryFactorsRequest
