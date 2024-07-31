package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UpsertPlanRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpsertPlanRequest(
      caseManager = "inani",
      reasonForPlan = "posidonium",
      firstCaseReviewDate = LocalDate.now(),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `caseManager must have no more than 100 characters`() {
    val request = UpsertPlanRequest(
      caseManager = "n".repeat(101),
      reasonForPlan = "posidonium",
      firstCaseReviewDate = LocalDate.now(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "caseManager",
      "Case Manager name must be <= 100 characters",
    )
  }

  @Test
  fun `reasonForPlan must have no more than 240 characters`() {
    val request = UpsertPlanRequest(
      caseManager = "inani",
      reasonForPlan = "n".repeat(241),
      firstCaseReviewDate = LocalDate.now(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "reasonForPlan",
      "Reason for Plan must be <= 240 characters",
    )
  }
}
