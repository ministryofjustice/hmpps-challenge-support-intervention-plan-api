package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UpdateCsipRecordRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpdateCsipRecordRequest(
      logCode = "menandri",
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `valid request with null logCode`() {
    val request = UpdateCsipRecordRequest(
      logCode = null,

    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = UpdateCsipRecordRequest(
      logCode = "n".repeat(11),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("logCode", "Log code must be <= 10 characters"),
    )
  }
}
