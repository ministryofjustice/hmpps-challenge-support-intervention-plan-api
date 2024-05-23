package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CreatePlanRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreatePlanRequest(
      caseManager = "inani",
      reasonForPlan = "posidonium",
      firstCaseReviewDate = LocalDate.now(),
      identifiedNeeds = listOf(
        CreateIdentifiedNeedRequest(
          identifiedNeed = "nobis",
          needIdentifiedBy = "vivamus",
          createdDate = LocalDate.now(),
          targetDate = LocalDate.now(),
          closedDate = null,
          intervention = "mutat",
          progression = null,
        ),
      ),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `CSIP Plan must have at least 1 identified need`() {
    val request = CreatePlanRequest(
      caseManager = "inani",
      reasonForPlan = "posidonium",
      firstCaseReviewDate = LocalDate.now(),
      identifiedNeeds = listOf(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "identifiedNeeds",
      "A CSIP Plan must have >=1 identified need(s).",
    )
  }

  @Test
  fun `caseManager must have no more than 100 characters`() {
    val request = CreatePlanRequest(
      caseManager = "n".repeat(101),
      reasonForPlan = "posidonium",
      firstCaseReviewDate = LocalDate.now(),
      identifiedNeeds = listOf(
        CreateIdentifiedNeedRequest(
          identifiedNeed = "nobis",
          needIdentifiedBy = "vivamus",
          createdDate = LocalDate.now(),
          targetDate = LocalDate.now(),
          closedDate = null,
          intervention = "mutat",
          progression = null,
        ),
      ),
    )
    assertSingleValidationError(
      validator.validate(request),
      "caseManager",
      "Case Manager name must be <= 100 characters",
    )
  }

  @Test
  fun `reasonForPlan must have no more than 240 characters`() {
    val request = CreatePlanRequest(
      caseManager = "inani",
      reasonForPlan = "n".repeat(241),
      firstCaseReviewDate = LocalDate.now(),
      identifiedNeeds = listOf(
        CreateIdentifiedNeedRequest(
          identifiedNeed = "nobis",
          needIdentifiedBy = "vivamus",
          createdDate = LocalDate.now(),
          targetDate = LocalDate.now(),
          closedDate = null,
          intervention = "mutat",
          progression = null,
        ),
      ),
    )
    assertSingleValidationError(
      validator.validate(request),
      "reasonForPlan",
      "Reason for Plan must be <= 240 characters",
    )
  }
}
