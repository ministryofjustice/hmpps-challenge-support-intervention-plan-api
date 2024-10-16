package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import java.time.LocalDate
import java.time.LocalTime

interface ReferralDateRequest {
  val referralDate: LocalDate
}

interface ReferralRequest {
  @get:Schema(description = "The date the incident that motivated the CSIP referral occurred", example = "2021-09-27")
  val incidentDate: LocalDate

  @get:Schema(description = "The time the incident that motivated the CSIP referral occurred", example = "14:19:25")
  val incidentTime: LocalTime?

  @get:Schema(description = "The type of incident that motivated the CSIP referral.")
  @get:Size(min = 1, max = 12, message = "Incident Type code must be <= 12 characters")
  val incidentTypeCode: String

  @get:Schema(description = "The location of incident that motivated the CSIP referral.")
  @get:Size(min = 1, max = 12, message = "Incident Location code must be <= 12 characters")
  val incidentLocationCode: String

  @get:Schema(description = "The person reporting the incident or creating the CSIP referral.")
  @get:Size(min = 1, max = 240, message = "Referer name must be <= 240 characters")
  val referredBy: String

  @get:Schema(description = "The area of work of the person reporting the incident or creating the CSIP referral.")
  @get:Size(min = 1, max = 12, message = "Area code must be <= 12 characters")
  val refererAreaCode: String

  @get:Schema(description = "Was this referral proactive or preventative.")
  val isProactiveReferral: Boolean?

  @get:Schema(description = "Were any members of staff assaulted in the incident.")
  val isStaffAssaulted: Boolean?

  @get:Schema(description = "Name or names of assaulted members of staff if any.")
  @get:Size(min = 0, max = 1000, message = "Name or names must be <= 1000 characters")
  val assaultedStaffName: String?

  @get:Schema(description = "The type of involvement the person had in the incident")
  @get:Size(min = 1, max = 12, message = "Involvement code must be <= 12 characters")
  val incidentInvolvementCode: String?

  @get:Schema(description = "The reasons why there is cause for concern.")
  @get:Size(min = 0, max = 4000, message = "Description of concern must be <= 4000 characters")
  val descriptionOfConcern: String?

  @get:Schema(description = "The reasons already known about the causes of the incident or motivation for CSIP referral.")
  @get:Size(min = 0, max = 4000, message = "Known reasons must be <= 4000 characters")
  val knownReasons: String?

  @get:Schema(description = "Any other information about the incident or reasons for CSIP referral.")
  @get:Size(min = 0, max = 4000, message = "Other information must be <= 4000 characters")
  val otherInformation: String?

  @get:Schema(description = "Records whether the safer custody team been informed.")
  val isSaferCustodyTeamInformed: OptionalYesNoAnswer

  @get:Schema(description = "Is the referral complete.")
  val isReferralComplete: Boolean?
}

interface ContributoryFactorRequest {
  @get:Schema(description = "The type of contributory factor to the incident or motivation for CSIP referral.")
  @get:Size(min = 1, max = 12, message = "Contributory factor type code must be <= 12 characters")
  val factorTypeCode: String

  @get:Schema(description = "Additional information about the contributory factor to the incident or motivation for CSIP referral.")
  @get:Size(max = 4000, message = "Comment must not be more than 4000 characters")
  val comment: String?
}

interface ContributoryFactorsRequest {
  @get:Schema(description = "Contributory factors to the incident that motivated the referral.")
  @get:Valid
  val contributoryFactors: List<ContributoryFactorRequest>
}

interface CompletableRequest {
  val completed: Boolean?
  val completedDate: LocalDate?

  @get:Size(min = 0, max = 64, message = "Completed by username must be <= 64 characters")
  val completedBy: String?

  @get:Size(min = 0, max = 255, message = "Completed by display name must be <= 255 characters")
  val completedByDisplayName: String?
}

fun CsipRequestContext.asCompletable(completed: Boolean?): CompletableRequest =
  object : CompletableRequest {
    override val completed: Boolean? = completed
    override val completedDate: LocalDate? = if (completed == true) requestAt.toLocalDate() else null
    override val completedBy: String? = if (completed == true) username else null
    override val completedByDisplayName: String? = if (completed == true) userDisplayName else null
  }
