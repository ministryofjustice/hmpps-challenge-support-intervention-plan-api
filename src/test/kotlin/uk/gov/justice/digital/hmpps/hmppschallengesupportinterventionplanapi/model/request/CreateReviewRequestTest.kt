package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreateReviewRequest

class CreateReviewRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateReviewRequest(
      reviewDate = null,
      recordedBy = "",
      recordedByDisplayName = "",
      nextReviewDate = null,
      csipClosedDate = null,
      summary = null,
      actions = setOf(),
      attendees = listOf(),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = CreateReviewRequest(
      reviewDate = null,
      recordedBy = "n".repeat(65),
      recordedByDisplayName = "n".repeat(256),
      nextReviewDate = null,
      actions = setOf(),
      csipClosedDate = null,
      summary = "n".repeat(4001),
      attendees = listOf(),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("recordedBy", "Recorded by username must be <= 64 characters"),
      Pair("recordedByDisplayName", "Recorded by display name must be <= 255 characters"),
      Pair("summary", "Summary must be <= 4000 characters"),
    )
  }
}
