package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.RequestValidationTest

class CreateReferenceDataRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateReferenceDataRequest(
      code = "senserit",
      description = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = CreateReferenceDataRequest(
      code = "n".repeat(13),
      description = "n".repeat(41),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("code", "Reference data code must be <= 12 characters"),
      Pair("description", "Reference data description must be <= 40 characters"),
    )
  }
}
