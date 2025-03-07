package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreateAttendeeRequest

class CreateAttendeeRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = createRequest()
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `name must be no more than 100 characters`() {
    val request = createRequest(name = "n".repeat(101))
    assertSingleValidationError(validator.validate(request), "name", "Attendee name must be <= 100 characters")
  }

  @Test
  fun `role must be no more than 100 characters`() {
    val request = createRequest(role = "n".repeat(51))
    assertSingleValidationError(validator.validate(request), "role", "Attendee role must be <= 50 characters")
  }

  @Test
  fun `contribution must be no more than 4000 characters`() {
    val request = createRequest(contribution = "n".repeat(4001))
    assertSingleValidationError(validator.validate(request), "contribution", "Contribution must be <= 4000 characters")
  }

  private fun createRequest(
    name: String? = "Name",
    role: String? = "Role",
    isAttended: Boolean? = true,
    contribution: String? = null,
  ): CreateAttendeeRequest = CreateAttendeeRequest(
    name = name,
    role = role,
    isAttended = isAttended,
    contribution = contribution,
  )
}
