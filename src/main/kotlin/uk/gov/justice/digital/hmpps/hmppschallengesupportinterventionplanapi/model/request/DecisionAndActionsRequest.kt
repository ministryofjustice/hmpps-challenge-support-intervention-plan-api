package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import java.time.LocalDate

interface DecisionAndActionsRequest {
  val conclusion: String?
  val outcomeTypeCode: String?
  val signedOffByRoleCode: String?
  val recordedBy: String?
  val recordedByDisplayName: String?
  val date: LocalDate?
  val nextSteps: String?
  val actionOther: String?
  val actions: Set<DecisionAction>
}
