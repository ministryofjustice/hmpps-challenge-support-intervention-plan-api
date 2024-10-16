package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpsertDecisionAndActionsRequest

class UpsertDecisionAndActionsRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpsertDecisionAndActionsRequest(
      conclusion = null,
      outcomeTypeCode = "suscipiantur",
      signedOffByRoleCode = "OTHER",
      recordedBy = null,
      recordedByDisplayName = null,
      date = null,
      nextSteps = null,
      actionOther = null,
      actions = setOf(),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = UpsertDecisionAndActionsRequest(
      conclusion = "n".repeat(4001),
      outcomeTypeCode = "n".repeat(13),
      signedOffByRoleCode = "n".repeat(13),
      recordedBy = "n".repeat(65),
      recordedByDisplayName = "n".repeat(256),
      date = null,
      nextSteps = "n".repeat(4001),
      actionOther = "n".repeat(4001),
      actions = setOf(),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("conclusion", "Conclusion must be <= 4000 characters"),
      Pair("outcomeTypeCode", "Decision outcome code must be <= 12 characters"),
      Pair("signedOffByRoleCode", "Signed off by role code must be <= 12 characters"),
      Pair("recordedBy", "Recorded by username must be <= 64 characters"),
      Pair("recordedByDisplayName", "Recorded by display name must be <= 255 characters"),
      Pair("nextSteps", "Next steps must be <= 4000 characters"),
      Pair("actionOther", "Action other must be <= 4000 characters"),
    )
  }
}
