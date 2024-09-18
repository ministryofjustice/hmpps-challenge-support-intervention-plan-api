package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import java.time.LocalDate
import java.time.LocalTime

interface ReferralDateRequest {
  val referralDate: LocalDate
}

interface ReferralRequest {
  val incidentDate: LocalDate
  val incidentTime: LocalTime?
  val incidentTypeCode: String
  val incidentLocationCode: String
  val referredBy: String
  val refererAreaCode: String
  val isProactiveReferral: Boolean?
  val isStaffAssaulted: Boolean?
  val assaultedStaffName: String?
  val incidentInvolvementCode: String?
  val descriptionOfConcern: String?
  val knownReasons: String?
  val otherInformation: String?
  val isSaferCustodyTeamInformed: OptionalYesNoAnswer
  val isReferralComplete: Boolean?
}

interface ContributoryFactorRequest {
  val factorTypeCode: String
  val comment: String?
}

interface ContributoryFactorsRequest {
  val contributoryFactors: List<ContributoryFactorRequest>
}

interface CompletableRequest {
  val completed: Boolean?
  val completedDate: LocalDate?
  val completedBy: String?
  val completedByDisplayName: String?
}

fun CsipRequestContext.asCompletable(completed: Boolean?): CompletableRequest =
  object : CompletableRequest {
    override val completed: Boolean? = completed
    override val completedDate: LocalDate? = if (completed == true) requestAt.toLocalDate() else null
    override val completedBy: String? = if (completed == true) username else null
    override val completedByDisplayName: String? = if (completed == true) userDisplayName else null
  }
