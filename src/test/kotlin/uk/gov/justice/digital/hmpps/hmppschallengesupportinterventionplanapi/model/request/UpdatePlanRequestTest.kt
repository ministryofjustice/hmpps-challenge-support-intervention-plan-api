package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UpdatePlanRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpdatePlanRequest(
      caseManager = "postulant",
      reasonForPlan = "verterem",
      firstCaseReviewDate = LocalDate.now(),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = UpdatePlanRequest(
      caseManager = "n".repeat(101),
      reasonForPlan = "n".repeat(241),
      firstCaseReviewDate = LocalDate.now(),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("reasonForPlan", "Reason for Plan must be <= 240 characters"),
      Pair("caseManager", "Case Manager name must be <= 100 characters"),
    )
  }
}
