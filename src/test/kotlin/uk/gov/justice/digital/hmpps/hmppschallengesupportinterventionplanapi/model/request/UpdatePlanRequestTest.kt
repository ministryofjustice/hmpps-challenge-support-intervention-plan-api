package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.UpdatePlanRequest
import java.time.LocalDate

class UpdatePlanRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpdatePlanRequest(
      caseManager = "inani",
      reasonForPlan = "posidonium",
      nextCaseReviewDate = LocalDate.now(),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `caseManager must have no more than 100 characters`() {
    val request = UpdatePlanRequest(
      caseManager = "n".repeat(101),
      reasonForPlan = "posidonium",
      nextCaseReviewDate = LocalDate.now(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "caseManager",
      "Case manager name must be <= 100 characters",
    )
  }

  @Test
  fun `reasonForPlan must have no more than 240 characters`() {
    val request = UpdatePlanRequest(
      caseManager = "inani",
      reasonForPlan = "n".repeat(241),
      nextCaseReviewDate = LocalDate.now(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "reasonForPlan",
      "Reason for plan must be <= 240 characters",
    )
  }
}
