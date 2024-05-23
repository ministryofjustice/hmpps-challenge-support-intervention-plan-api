package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreateReviewRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateReviewRequest(
      reviewDate = null,
      recordedBy = "",
      recordedByDisplayName = "",
      nextReviewDate = null,
      isActionResponsiblePeopleInformed = null,
      isActionCsipUpdated = null,
      isActionRemainOnCsip = null,
      isActionCaseNote = null,
      isActionCloseCsip = null,
      csipClosedDate = null,
      summary = null,
      attendees = listOf(),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = CreateReviewRequest(
      reviewDate = null,
      recordedBy = "n".repeat(33),
      recordedByDisplayName = "n".repeat(256),
      nextReviewDate = null,
      isActionResponsiblePeopleInformed = null,
      isActionCsipUpdated = null,
      isActionRemainOnCsip = null,
      isActionCaseNote = null,
      isActionCloseCsip = null,
      csipClosedDate = null,
      summary = "n".repeat(4001),
      attendees = listOf(),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("recordedBy", "Recorded By username must be <= 32 characters"),
      Pair("recordedByDisplayName", "Recorded By display name must be <= 255 characters"),
      Pair("summary", "Summary must be <= 4000 characters"),
    )
  }
}
