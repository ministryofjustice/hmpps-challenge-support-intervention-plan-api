package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

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
  val completedDate: LocalDate?
  val completedBy: String?
  val completedByDisplayName: String?
}

interface ContributoryFactorRequest : CommentRequest {
  val factorTypeCode: String
}

interface ContributoryFactorsRequest {
  val contributoryFactors: List<ContributoryFactorRequest>
}

interface CommentRequest {
  val comment: String?
}
