package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import java.time.LocalDate

interface IdentifiedNeedRequest {
  val identifiedNeed: String
  val responsiblePerson: String
  val createdDate: LocalDate
  val targetDate: LocalDate
  val closedDate: LocalDate?
  val intervention: String
  val progression: String?
}

interface IdentifiedNeedsRequest {
  val identifiedNeeds: List<IdentifiedNeedRequest>
}
