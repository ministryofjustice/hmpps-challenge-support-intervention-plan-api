package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UpdateReviewRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpdateReviewRequest(
      reviewDate = null,
      recordedBy = "quaerendum",
      recordedByDisplayName = "Julie Nicholson",
      nextReviewDate = null,
      isActionResponsiblePeopleInformed = null,
      isActionCsipUpdated = null,
      isActionRemainOnCsip = null,
      isActionCaseNote = null,
      isActionCloseCsip = null,
      csipClosedDate = null,
      summary = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = UpdateReviewRequest(
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
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("recordedBy", "Recorded By username must be <= 32 characters"),
      Pair("recordedByDisplayName", "Recorded By display name must be <= 255 characters"),
      Pair("summary", "Summary must be <= 4000 characters"),
    )
  }
}
