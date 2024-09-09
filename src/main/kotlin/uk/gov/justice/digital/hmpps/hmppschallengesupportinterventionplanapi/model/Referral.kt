package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "The referral of a CSIP record")
data class Referral(
  @Schema(description = "The date the incident that motivated the CSIP referral occurred", example = "2021-09-27")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val incidentDate: LocalDate,

  @Schema(description = "The time the incident that motivated the CSIP referral occurred", example = "14:19:25")
  @JsonFormat(pattern = "HH:mm:ss")
  val incidentTime: LocalTime?,

  @Schema(description = "The type of incident that motivated the CSIP referral.")
  val incidentType: ReferenceData,

  @Schema(description = "The location of incident that motivated the CSIP referral.")
  val incidentLocation: ReferenceData,

  @Schema(description = "The person reporting the incident or creating the CSIP referral.")
  val referredBy: String,

  @Schema(description = "The area of work of the person reporting the incident or creating the CSIP referral.")
  val refererArea: ReferenceData,

  @Schema(description = "Was this referral proactive or preventative.")
  val isProactiveReferral: Boolean?,

  @Schema(description = "Were any members of staff assaulted in the incident.")
  val isStaffAssaulted: Boolean?,

  @Schema(description = "Name or names of assaulted members of staff if any.")
  val assaultedStaffName: String?,

  @Schema(description = "The type of involvement the person had in the incident")
  val incidentInvolvement: ReferenceData?,

  @Schema(description = "The reasons why there is cause for concern.")
  val descriptionOfConcern: String?,

  @Schema(description = "The reasons already known about the causes of the incident or motivation for CSIP referral.")
  val knownReasons: String?,

  @Schema(description = "Any other information about the incident or reasons for CSIP referral.")
  val otherInformation: String?,

  @Schema(description = "Records whether the safer custody team been informed.")
  val isSaferCustodyTeamInformed: OptionalYesNoAnswer,

  @Schema(description = "Is the referral complete.")
  val isReferralComplete: Boolean?,

  @Schema(description = "The date the referral was completed.")
  val referralCompletedDate: LocalDate?,

  @Schema(description = "The username of the person completing the referral")
  val referralCompletedBy: String?,

  @Schema(description = "The name of the person completing the referral")
  val referralCompletedByDisplayName: String?,

  @Schema(description = "Contributory factors to the incident that motivated the referral.")
  val contributoryFactors: Collection<ContributoryFactor>,

  @Schema(description = "The investigation on the incident that motivated the CSIP referral.")
  val investigation: Investigation?,

  @Schema(description = "The Safer Custody Screening Outcome for the CSIP referral.")
  val saferCustodyScreeningOutcome: SaferCustodyScreeningOutcome?,

  @Schema(description = "The Decision and Actions for the CSIP referral.")
  val decisionAndActions: DecisionAndActions?,
)
