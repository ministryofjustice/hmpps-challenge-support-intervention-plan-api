package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.UpdateAttendeeRequest

class UpdateAttendeeRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = updateRequest()
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `test field validations`() {
    val request = updateRequest(name = "n".repeat(101), role = "n".repeat(51), contribution = "n".repeat(4001))
    assertValidationErrors(
      validator.validate(request),
      "name" to "Attendee name must be <= 100 characters",
      "role" to "Attendee role must be <= 50 characters",
      "contribution" to "Contribution must be <= 4000 characters",
    )
  }

  private fun updateRequest(
    name: String? = "Name",
    role: String? = "Role",
    isAttended: Boolean? = true,
    contribution: String? = null,
  ): UpdateAttendeeRequest = UpdateAttendeeRequest(
    name = name,
    role = role,
    isAttended = isAttended,
    contribution = contribution,
  )
}
