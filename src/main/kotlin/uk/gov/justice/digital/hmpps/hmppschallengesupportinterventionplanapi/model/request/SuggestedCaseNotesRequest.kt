package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType

data class SuggestedCaseNotesRequest(
  val referralId: String,
  val behaviourType: BehaviourType,
  val sortField: String,
  val sortOrder: String,
)
