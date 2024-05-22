package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InterviewRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = InterviewRequest(
      interviewee = "ei",
      interviewDate = LocalDate.now(),
      intervieweeRoleCode = "et",
      interviewText = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = InterviewRequest(
      interviewee = "n".repeat(101),
      interviewDate = LocalDate.now(),
      intervieweeRoleCode = "n".repeat(13),
      interviewText = "n".repeat(4001),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("interviewText", "Interview Text must be <= 4000 characters"),
      Pair("interviewee", "Interviewee name must be <= 100 characters"),
      Pair("intervieweeRoleCode", "Interviewee Role Code must be <= 12 characters"),
    )
  }
}
