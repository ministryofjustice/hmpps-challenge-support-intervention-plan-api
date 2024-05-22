package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class CreateReferralRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateReferralRequest(
      incidentDate = LocalDate.now(),
      incidentTime = LocalTime.now(),
      incidentTypeCode = "idque",
      incidentLocationCode = "ridiculus",
      referredBy = "maximus",
      refererAreaCode = "intellegat",
      referralSummary = null,
      isProactiveReferral = null,
      isStaffAssaulted = null,
      assaultedStaffName = null,
      incidentInvolvementCode = "vidisse",
      descriptionOfConcern = "molestie",
      knownReasons = "dicat",
      otherInformation = null,
      isSaferCustodyTeamInformed = null,
      isReferralComplete = null,
      contributoryFactors = listOf(
        ContributoryFactorRequest(
          factorTypeCode = "pericula",
          comment = null,
        ),
      ),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = CreateReferralRequest(
      incidentDate = LocalDate.now(),
      incidentTime = LocalTime.now(),
      incidentTypeCode = "n".repeat(13),
      incidentLocationCode = "n".repeat(41),
      referredBy = "n".repeat(241),
      refererAreaCode = "n".repeat(41),
      referralSummary = "n".repeat(4001),
      isProactiveReferral = null,
      isStaffAssaulted = null,
      assaultedStaffName = "n".repeat(1001),
      incidentInvolvementCode = "n".repeat(13),
      descriptionOfConcern = "molestie",
      knownReasons = "dicat",
      otherInformation = null,
      isSaferCustodyTeamInformed = null,
      isReferralComplete = null,
      contributoryFactors = listOf(
        ContributoryFactorRequest(
          factorTypeCode = "pericula",
          comment = null,
        ),
      ),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("incidentLocationCode", "Incident Location code must be <= 40 characters"),
      Pair("incidentTypeCode", "Incident Type code must be <= 12 characters"),
      Pair("referredBy", "Referer name must be <= 240 characters"),
      Pair("refererAreaCode", "Area code must be <= 40 characters"),
      Pair("referralSummary", "Summary must be <= 4000 characters"),
      Pair("incidentInvolvementCode", "Involvement code must be <= 12 characters"),
      Pair("assaultedStaffName", "Name or names must be <= 1000 characters"),
    )
  }

  @Test
  fun `validation fails if referral request does not have at least one contributory factor`() {
    val request = CreateReferralRequest(
      incidentDate = LocalDate.now(),
      incidentTime = LocalTime.now(),
      incidentTypeCode = "idque",
      incidentLocationCode = "ridiculus",
      referredBy = "maximus",
      refererAreaCode = "intellegat",
      referralSummary = null,
      isProactiveReferral = null,
      isStaffAssaulted = null,
      assaultedStaffName = null,
      incidentInvolvementCode = "vidisse",
      descriptionOfConcern = "molestie",
      knownReasons = "dicat",
      otherInformation = null,
      isSaferCustodyTeamInformed = null,
      isReferralComplete = null,
      contributoryFactors = listOf(),
    )
    assertSingleValidationError(
      validator.validate(request),
      "contributoryFactors",
      "A referral must have >=1 contributory factor(s).",
    )
  }
}
