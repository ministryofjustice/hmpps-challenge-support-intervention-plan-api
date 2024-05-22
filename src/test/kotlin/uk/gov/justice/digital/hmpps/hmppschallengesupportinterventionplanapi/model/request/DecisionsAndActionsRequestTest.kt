package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DecisionsAndActionsRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = DecisionsAndActionsRequest(
      conclusion = null,
      outcomeTypeCode = "suscipiantur",
      outcomeSignedOffByRoleCode = null,
      outcomeRecordedBy = null,
      outcomeRecordedByDisplayName = null,
      outcomeDate = null,
      nextSteps = null,
      isActionOpenCsipAlert = null,
      isActionNonAssociationsUpdated = null,
      isActionObservationBook = null,
      isActionUnitOrCellMove = null,
      isActionCsraOrRsraReview = null,
      isActionServiceReferral = null,
      isActionSimReferral = null,
      actionOther = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = DecisionsAndActionsRequest(
      conclusion = "n".repeat(4001),
      outcomeTypeCode = "n".repeat(13),
      outcomeSignedOffByRoleCode = "n".repeat(13),
      outcomeRecordedBy = "n".repeat(101),
      outcomeRecordedByDisplayName = "n".repeat(256),
      outcomeDate = null,
      nextSteps = "n".repeat(4001),
      isActionOpenCsipAlert = null,
      isActionNonAssociationsUpdated = null,
      isActionObservationBook = null,
      isActionUnitOrCellMove = null,
      isActionCsraOrRsraReview = null,
      isActionServiceReferral = null,
      isActionSimReferral = null,
      actionOther = "n".repeat(4001),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("conclusion", "Conclusion must be <= 4000 characters"),
      Pair("outcomeTypeCode", "Outcome Type code must be <= 12 characters"),
      Pair("outcomeSignedOffByRoleCode", "Outcome Sign Off By Role code must be <= 12 characters"),
      Pair("outcomeRecordedBy", "Outcome Recorded By username must be <= 100 characters"),
      Pair("outcomeRecordedByDisplayName", "Outcome Recorded By display name must be <= 255 characters"),
      Pair("nextSteps", "Next Step must be <= 4000 characters"),
      Pair("actionOther", "Action Other must be <= 4000 characters"),
    )
  }
}
