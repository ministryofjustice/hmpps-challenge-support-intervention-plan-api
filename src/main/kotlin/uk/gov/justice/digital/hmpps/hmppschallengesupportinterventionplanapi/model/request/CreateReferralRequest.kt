package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "The request body for creating a CSIP referral")
data class CreateReferralRequest(
  @Schema(description = "The date the incident that motivated the CSIP referral occurred", example = "2021-09-27")
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val incidentDate: LocalDate,

  @Schema(
    description = "The time the incident that motivated the CSIP referral occurred",
    example = "14:19:25",
  )
  @JsonFormat(pattern = "HH:mm:ss")
  override val incidentTime: LocalTime?,

  @Schema(description = "The type of incident that motivated the CSIP referral.")
  @field:Size(min = 1, max = 12, message = "Incident Type code must be <= 12 characters")
  override val incidentTypeCode: String,

  @Schema(description = "The location of incident that motivated the CSIP referral.")
  @field:Size(min = 1, max = 12, message = "Incident Location code must be <= 12 characters")
  override val incidentLocationCode: String,

  @Schema(description = "The person reporting the incident or creating the CSIP referral.")
  @field:Size(min = 1, max = 240, message = "Referer name must be <= 240 characters")
  override val referredBy: String,

  @Schema(description = "The area of work of the person reporting the incident or creating the CSIP referral.")
  @field:Size(min = 1, max = 12, message = "Area code must be <= 12 characters")
  override val refererAreaCode: String,

  @Schema(description = "Was this referral proactive or preventative.")
  override val isProactiveReferral: Boolean?,

  @Schema(description = "Were any members of staff assaulted in the incident.")
  override val isStaffAssaulted: Boolean?,

  @Schema(description = "Name or names of assaulted members of staff if any.")
  @field:Size(min = 0, max = 1000, message = "Name or names must be <= 1000 characters")
  override val assaultedStaffName: String?,

  @Schema(description = "The type of involvement the person had in the incident")
  @field:Size(min = 1, max = 12, message = "Involvement code must be <= 12 characters")
  override val incidentInvolvementCode: String?,

  @Schema(description = "The reasons why there is cause for concern.")
  @field:Size(min = 0, max = 4000, message = "Description of concern must be <= 4000 characters")
  override val descriptionOfConcern: String?,

  @Schema(description = "The reasons already known about the causes of the incident or motivation for CSIP referral.")
  @field:Size(min = 0, max = 4000, message = "Known reasons must be <= 4000 characters")
  override val knownReasons: String?,

  @Schema(description = "Any other information about the incident or reasons for CSIP referral.")
  @field:Size(min = 0, max = 4000, message = "Other information must be <= 4000 characters")
  override val otherInformation: String?,

  @Schema(description = "Records whether the safer custody team been informed.")
  override val isSaferCustodyTeamInformed: OptionalYesNoAnswer,

  @Schema(description = "Is the referral complete.")
  override val isReferralComplete: Boolean?,

  @Schema(
    description = "The date the referral was completed.",
    example = "2024-07-29",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val completedDate: LocalDate?,

  @Schema(description = "The username of the person who completed the referral.")
  @field:Size(min = 0, max = 64, message = "Completed by username must be <= 64 characters")
  override val completedBy: String?,

  @Schema(description = "The displayable name of the person who completed the referral.")
  @field:Size(min = 0, max = 255, message = "Completed by display name must be <= 255 characters")
  override val completedByDisplayName: String?,

  @Schema(description = "Contributory factors to the incident that motivated the referral.")
  @field:Valid
  @field:NotEmpty(message = "A referral must have at least one contributory factor.")
  override val contributoryFactors: List<CreateContributoryFactorRequest>,
) : ReferralRequest, ContributoryFactorsRequest
