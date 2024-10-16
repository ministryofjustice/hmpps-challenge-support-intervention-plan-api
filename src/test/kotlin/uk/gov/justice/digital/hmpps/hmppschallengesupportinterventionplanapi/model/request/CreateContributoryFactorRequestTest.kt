package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateContributoryFactorRequest

class CreateContributoryFactorRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateContributoryFactorRequest(factorTypeCode = "CODE", comment = "none")
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `Contributory factor type code must be no more than 12 characters`() {
    val request = CreateContributoryFactorRequest(
      factorTypeCode = "n".repeat(13),
      comment = "n".repeat(4001),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("factorTypeCode", "Contributory factor type code must be <= 12 characters"),
      Pair("comment", "Comment must not be more than 4000 characters"),
    )
  }
}
