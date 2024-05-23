package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreateInvestigationRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateInvestigationRequest(
      staffInvolved = null,
      evidenceSecured = null,
      occurrenceReason = null,
      personsUsualBehaviour = null,
      personsTrigger = null,
      protectiveFactors = null,
      interviews = listOf(),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `staffInvolved must be no more than 4000 characters`() {
    val request = CreateInvestigationRequest(
      staffInvolved = "n".repeat(4001),
      evidenceSecured = null,
      occurrenceReason = null,
      personsUsualBehaviour = null,
      personsTrigger = null,
      protectiveFactors = null,
      interviews = listOf(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "staffInvolved",
      "Staff involved must be <= 4000 characters",
    )
  }

  @Test
  fun `evidenceSecured must be no more than 4000 characters`() {
    val request = CreateInvestigationRequest(
      staffInvolved = null,
      evidenceSecured = "n".repeat(4001),
      occurrenceReason = null,
      personsUsualBehaviour = null,
      personsTrigger = null,
      protectiveFactors = null,
      interviews = listOf(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "evidenceSecured",
      "Evidence Secured must be <= 4000 characters",
    )
  }

  @Test
  fun `occurrenceReason must be no more than 4000 characters`() {
    val request = CreateInvestigationRequest(
      staffInvolved = null,
      evidenceSecured = null,
      occurrenceReason = "n".repeat(4001),
      personsUsualBehaviour = null,
      personsTrigger = null,
      protectiveFactors = null,
      interviews = listOf(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "occurrenceReason",
      "Occurrence reason must be <= 4000 characters",
    )
  }

  @Test
  fun `personsUsualBehaviour must be no more than 4000 characters`() {
    val request = CreateInvestigationRequest(
      staffInvolved = null,
      evidenceSecured = null,
      occurrenceReason = null,
      personsUsualBehaviour = "n".repeat(4001),
      personsTrigger = null,
      protectiveFactors = null,
      interviews = listOf(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "personsUsualBehaviour",
      "Person's Usual Behaviour must be <= 4000 characters",
    )
  }

  @Test
  fun `personsTrigger must be no more than 4000 characters`() {
    val request = CreateInvestigationRequest(
      staffInvolved = null,
      evidenceSecured = null,
      occurrenceReason = null,
      personsUsualBehaviour = null,
      personsTrigger = "n".repeat(4001),
      protectiveFactors = null,
      interviews = listOf(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "personsTrigger",
      "Person's Trigger must be <= 4000 characters",
    )
  }

  @Test
  fun `protectiveFactors must be no more than 4000 characters`() {
    val request = CreateInvestigationRequest(
      staffInvolved = null,
      evidenceSecured = null,
      occurrenceReason = null,
      personsUsualBehaviour = null,
      personsTrigger = null,
      protectiveFactors = "n".repeat(4001),
      interviews = listOf(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "protectiveFactors",
      "Protective Factors must be <= 4000 characters",
    )
  }
}
