package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.UpdateReviewRequest
import java.time.LocalDate

class UpdateReviewRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpdateReviewRequest(
      reviewDate = LocalDate.now(),
      recordedBy = "quaerendum",
      recordedByDisplayName = "Julie Nicholson",
      nextReviewDate = null,
      actions = sortedSetOf(),
      csipClosedDate = null,
      summary = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = UpdateReviewRequest(
      reviewDate = LocalDate.now(),
      recordedBy = "n".repeat(65),
      recordedByDisplayName = "n".repeat(256),
      nextReviewDate = null,
      actions = sortedSetOf(),
      csipClosedDate = null,
      summary = "n".repeat(4001),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("recordedBy", "Recorded by username must be <= 64 characters"),
      Pair("recordedByDisplayName", "Recorded by display name must be <= 255 characters"),
      Pair("summary", "Summary must be <= 4000 characters"),
    )
  }
}
