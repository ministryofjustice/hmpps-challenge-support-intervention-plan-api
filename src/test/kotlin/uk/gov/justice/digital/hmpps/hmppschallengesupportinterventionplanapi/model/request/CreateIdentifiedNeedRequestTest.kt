package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CreateIdentifiedNeedRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateIdentifiedNeedRequest(
      identifiedNeed = "qui",
      needIdentifiedBy = "melius",
      createdDate = LocalDate.now(),
      targetDate = LocalDate.now(),
      closedDate = null,
      intervention = "arcu",
      progression = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = CreateIdentifiedNeedRequest(
      identifiedNeed = "n".repeat(1001),
      needIdentifiedBy = "n".repeat(101),
      createdDate = LocalDate.now(),
      targetDate = LocalDate.now(),
      closedDate = null,
      intervention = "n".repeat(4001),
      progression = "n".repeat(4001),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("progression", "Progression must be <= 4000 characters"),
      Pair("identifiedNeed", "Identified Need must be <= 1000 characters"),
      Pair("intervention", "Intervention must be <= 4000 characters"),
      Pair("needIdentifiedBy", "Need Identified By name must be <= 100 characters"),
    )
  }
}
