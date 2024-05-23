package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class CreateCsipRecordRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateCsipRecordRequest(
      logNumber = "tamquam",
      referral = CreateReferralRequest(
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
          CreateContributoryFactorRequest(
            factorTypeCode = "pericula",
            comment = null,
          ),
        ),
      ),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `Log number must be no more than 10 characters`() {
    val request = CreateCsipRecordRequest(
      logNumber = "n".repeat(11),
      referral = CreateReferralRequest(
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
          CreateContributoryFactorRequest(
            factorTypeCode = "pericula",
            comment = null,
          ),
        ),
      ),
    )
    assertSingleValidationError(validator.validate(request), "logNumber", "Log number must be <= 10 characters")
  }
}
