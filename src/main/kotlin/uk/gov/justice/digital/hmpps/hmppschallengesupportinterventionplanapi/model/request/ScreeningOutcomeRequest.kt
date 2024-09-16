package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import java.time.LocalDate

interface ScreeningOutcomeRequest {
  val outcomeTypeCode: String
  val reasonForDecision: String?
  val date: LocalDate
  val recordedBy: String
  val recordedByDisplayName: String
}
