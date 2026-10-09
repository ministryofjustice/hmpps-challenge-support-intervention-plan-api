package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import java.util.UUID

data class SuggestedCaseNotesRequest(
  @field:NotNull(message = "referralId is required")
  val referralId: UUID,
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
