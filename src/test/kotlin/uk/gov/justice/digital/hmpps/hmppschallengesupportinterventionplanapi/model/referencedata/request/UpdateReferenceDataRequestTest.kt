package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.RequestValidationTest

class UpdateReferenceDataRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpdateReferenceDataRequest(
      description = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = UpdateReferenceDataRequest(
      description = "n".repeat(41),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("description", "Reference data description must be <= 40 characters"),
    )
  }
}
