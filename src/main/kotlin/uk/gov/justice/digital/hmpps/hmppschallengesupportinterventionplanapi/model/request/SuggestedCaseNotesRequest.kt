package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType

data class SuggestedCaseNotesRequest(
  val behaviourType: BehaviourType,
  var sortField: String = "createdDate",
  val sortOrder: String = "desc",
) {
  init {
    if (sortField == "createdDate" || sortField.isBlank()) {
      sortField = "creationDateTime"
    }
  }
}
