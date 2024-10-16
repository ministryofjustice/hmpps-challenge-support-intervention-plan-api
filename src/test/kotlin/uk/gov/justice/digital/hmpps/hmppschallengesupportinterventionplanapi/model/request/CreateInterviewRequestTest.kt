package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateInterviewRequest
import java.time.LocalDate

class CreateInterviewRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateInterviewRequest(
      interviewee = "ei",
      interviewDate = LocalDate.now(),
      intervieweeRoleCode = "et",
      interviewText = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = CreateInterviewRequest(
      interviewee = "n".repeat(101),
      interviewDate = LocalDate.now(),
      intervieweeRoleCode = "n".repeat(13),
      interviewText = "n".repeat(4001),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("interviewText", "Interview text must be <= 4000 characters"),
      Pair("interviewee", "Interviewee name must be <= 100 characters"),
      Pair("intervieweeRoleCode", "Interviewee role code must be <= 12 characters"),
    )
  }
}
