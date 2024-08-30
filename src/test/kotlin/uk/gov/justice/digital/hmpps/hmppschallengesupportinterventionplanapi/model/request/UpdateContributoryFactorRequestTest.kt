package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UpdateContributoryFactorRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpdateContributoryFactorRequest(comment = "A small comment")
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `Contributory factor type code must be no more than 12 characters`() {
    val request = UpdateContributoryFactorRequest(comment = "n".repeat(4001))
    assertValidationErrors(
      validator.validate(request),
      Pair("comment", "Comment must be less than 4000 characters"),
    )
  }
}
