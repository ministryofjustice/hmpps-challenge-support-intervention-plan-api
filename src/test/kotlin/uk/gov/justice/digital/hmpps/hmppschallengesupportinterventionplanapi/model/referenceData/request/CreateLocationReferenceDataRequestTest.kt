package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.RequestValidationTest

class CreateLocationReferenceDataRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateLocationReferenceDataRequest(
      code = "senserit",
      description = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = CreateLocationReferenceDataRequest(
      code = "n".repeat(41),
      description = "n".repeat(41),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("code", "Reference data code must be <= 40 characters"),
      Pair("description", "Reference data description must be <= 40 characters"),
    )
  }
}
