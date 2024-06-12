package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreateDecisionAndActionsRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateDecisionAndActionsRequest(
      conclusion = null,
      outcomeTypeCode = "suscipiantur",
      outcomeSignedOffByRoleCode = null,
      outcomeRecordedBy = null,
      outcomeRecordedByDisplayName = null,
      outcomeDate = null,
      nextSteps = null,
      isActionOpenCsipAlert = false,
      isActionNonAssociationsUpdated = false,
      isActionObservationBook = false,
      isActionUnitOrCellMove = false,
      isActionCsraOrRsraReview = false,
      isActionServiceReferral = false,
      isActionSimReferral = false,
      actionOther = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = CreateDecisionAndActionsRequest(
      conclusion = "n".repeat(4001),
      outcomeTypeCode = "n".repeat(13),
      outcomeSignedOffByRoleCode = "n".repeat(13),
      outcomeRecordedBy = "n".repeat(101),
      outcomeRecordedByDisplayName = "n".repeat(256),
      outcomeDate = null,
      nextSteps = "n".repeat(4001),
      isActionOpenCsipAlert = false,
      isActionNonAssociationsUpdated = false,
      isActionObservationBook = false,
      isActionUnitOrCellMove = false,
      isActionCsraOrRsraReview = false,
      isActionServiceReferral = false,
      isActionSimReferral = false,
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
