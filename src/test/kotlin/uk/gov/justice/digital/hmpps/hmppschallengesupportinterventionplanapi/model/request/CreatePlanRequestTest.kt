package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CreatePlanRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreatePlanRequest(
      caseManager = "postulant",
      reasonForPlan = "verterem",
      nextCaseReviewDate = LocalDate.now(),
      listOf(),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = CreatePlanRequest(
      caseManager = "n".repeat(101),
      reasonForPlan = "n".repeat(241),
      nextCaseReviewDate = LocalDate.now(),
      listOf(),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("reasonForPlan", "Reason for Plan must be <= 240 characters"),
      Pair("caseManager", "Case Manager name must be <= 100 characters"),
    )
  }
}
