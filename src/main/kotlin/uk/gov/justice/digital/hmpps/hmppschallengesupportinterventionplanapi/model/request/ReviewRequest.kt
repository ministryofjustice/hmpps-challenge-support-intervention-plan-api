package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import java.time.LocalDate

interface ReviewRequest {
  val reviewDate: LocalDate?
  val recordedBy: String
  val recordedByDisplayName: String
  val nextReviewDate: LocalDate?
  val csipClosedDate: LocalDate?
  val summary: String?
  val actions: Set<ReviewAction>
}

interface AttendeeRequest {
  val name: String?
  val role: String?
  val isAttended: Boolean?
  val contribution: String?
}

interface AttendeesRequest {
  val attendees: List<AttendeeRequest>
}
