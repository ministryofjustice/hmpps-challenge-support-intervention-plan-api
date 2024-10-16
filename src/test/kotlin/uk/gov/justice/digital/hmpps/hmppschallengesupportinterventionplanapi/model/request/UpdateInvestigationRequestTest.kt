package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpdateInvestigationRequest

class UpdateInvestigationRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpdateInvestigationRequest(
      staffInvolved = null,
      evidenceSecured = null,
      occurrenceReason = "occurrenceReason",
      personsUsualBehaviour = null,
      personsTrigger = null,
      protectiveFactors = null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = UpdateInvestigationRequest(
      staffInvolved = "n".repeat(4001),
      evidenceSecured = "n".repeat(4001),
      occurrenceReason = "n".repeat(4001),
      personsUsualBehaviour = "n".repeat(4001),
      personsTrigger = "n".repeat(4001),
      protectiveFactors = "n".repeat(4001),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("staffInvolved", "Staff involved must be <= 4000 characters"),
      Pair("personsUsualBehaviour", "Person's usual behaviour must be <= 4000 characters"),
      Pair("protectiveFactors", "Protective factors must be <= 4000 characters"),
      Pair("occurrenceReason", "Occurrence reason must be <= 4000 characters"),
      Pair("personsTrigger", "Person's trigger must be <= 4000 characters"),
      Pair("evidenceSecured", "Evidence secured must be <= 4000 characters"),
    )
  }
}
