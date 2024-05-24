package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description = "The request body for creating a CSIP referral",
)
data class CreateReferralRequest(
  @Schema(
    description = "The date the incident that motivated the CSIP referral occurred",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val incidentDate: LocalDate,

  @Schema(
    description = "The time the incident that motivated the CSIP referral occurred",
    example = "14:19:25",
    type = "String",
    pattern = "HH:mm:SS",
  )
  @JsonFormat(pattern = "HH:mm:ss")
  val incidentTime: LocalTime?,

  @Schema(
    description = "The type of incident that motivated the CSIP referral.",
  )
  @field:Size(min = 1, max = 12, message = "Incident Type code must be <= 12 characters")
  val incidentTypeCode: String,

  @Schema(
    description = "The location of incident that motivated the CSIP referral.",
  )
  @field:Size(min = 1, max = 12, message = "Incident Location code must be <= 12 characters")
  val incidentLocationCode: String,

  @Schema(
    description = "The person reporting the incident or creating the CSIP referral.",
  )
  @field:Size(min = 1, max = 240, message = "Referer name must be <= 240 characters")
  val referredBy: String,

  @Schema(
    description = "The area of work of the person reporting the incident or creating the CSIP referral.",
  )
  @field:Size(min = 1, max = 12, message = "Area code must be <= 12 characters")
  val refererAreaCode: String,

  @Schema(
    description = "Summary of the CSIP referral.",
  )
  @field:Size(min = 0, max = 4000, message = "Summary must be <= 4000 characters")
  val referralSummary: String?,

  @Schema(
    description = "Was this referral proactive or preventative.",
  )
  val isProactiveReferral: Boolean?,

  @Schema(
    description = "Were any members of staff assaulted in the incident.",
  )
  val isStaffAssaulted: Boolean?,

  @Schema(
    description = "Name or names of assaulted members of staff if any.",
  )
  @field:Size(min = 0, max = 1000, message = "Name or names must be <= 1000 characters")
  val assaultedStaffName: String?,

  @Schema(
    description = "The type of involvement the person had in the incident",
  )
  @field:Size(min = 1, max = 12, message = "Involvement code must be <= 12 characters")
  val incidentInvolvementCode: String,

  @Schema(
    description = "The reasons why there is cause for concern.",
  )
  val descriptionOfConcern: String,

  @Schema(
    description = "The reasons already known about the causes of the incident or motivation for CSIP referral.",
  )
  val knownReasons: String,

  @Schema(
    description = "Any other information about the incident or reasons for CSIP referral.",
  )
  val otherInformation: String?,

  @Schema(
    description = "Records whether the safer custody team been informed.",
  )
  val isSaferCustodyTeamInformed: Boolean?,

  @Schema(
    description = "Is the referral complete.",
  )
  val isReferralComplete: Boolean?,

  @Schema(
    description = "Contributory factors to the incident that motivated the referral.",
  )
  @field:Size(min = 1, message = "A referral must have >=1 contributory factor(s).")
  @field:Valid
  val contributoryFactors: Collection<CreateContributoryFactorRequest>,
)
