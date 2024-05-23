package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UpdateCsipRecordRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpdateCsipRecordRequest(
      logNumber = "menandri",
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = UpdateCsipRecordRequest(
      logNumber = "n".repeat(11),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("logNumber", "Log number must be <= 10 characters"),
    )
  }
}
