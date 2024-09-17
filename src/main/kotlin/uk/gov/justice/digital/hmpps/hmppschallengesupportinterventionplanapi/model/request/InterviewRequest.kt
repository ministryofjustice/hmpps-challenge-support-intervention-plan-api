package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import java.time.LocalDate

interface InterviewRequest {
  val interviewee: String
  val interviewDate: LocalDate
  val intervieweeRoleCode: String
  val interviewText: String?
}

interface InterviewsRequest {
  val interviews: List<InterviewRequest>
}
