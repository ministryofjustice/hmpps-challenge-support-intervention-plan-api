package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CreateSaferCustodyScreeningOutcomeRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateSaferCustodyScreeningOutcomeRequest(
      outcomeTypeCode = "viverra",
      recordBy = "eget",
      recordByDisplayName = "Charles Alford",
      date = LocalDate.now(),
      reasonForDecision = "detraxit",
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = CreateSaferCustodyScreeningOutcomeRequest(
      outcomeTypeCode = "n".repeat(13),
      recordBy = "n".repeat(101),
      recordByDisplayName = "n".repeat(256),
      date = LocalDate.now(),
      reasonForDecision = "n".repeat(4001),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("recordByDisplayName", "Recorder display name must be <= 255 characters"),
      Pair("recordBy", "Recorder username must be <= 100 characters"),
      Pair("reasonForDecision", "Reason for Decision must be <= 4000 characters"),
      Pair("outcomeTypeCode", "Outcome Type code must be <= 12 characters"),
    )
  }
}
