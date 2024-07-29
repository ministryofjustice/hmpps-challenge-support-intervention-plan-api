package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.DO_NOT_KNOW
import java.time.LocalDate
import java.time.LocalTime

class CreateCsipRecordRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = CreateCsipRecordRequest(
      logCode = "tamquam",
      referral = CreateReferralRequest(
        incidentDate = LocalDate.now(),
        incidentTime = LocalTime.now(),
        incidentTypeCode = "idque",
        incidentLocationCode = "ridiculus",
        referredBy = "maximus",
        refererAreaCode = "intellegat",
        isProactiveReferral = null,
        isStaffAssaulted = null,
        assaultedStaffName = null,
        incidentInvolvementCode = "vidisse",
        descriptionOfConcern = "molestie",
        knownReasons = "dicat",
        otherInformation = null,
        isSaferCustodyTeamInformed = DO_NOT_KNOW,
        isReferralComplete = null,
        completedDate = null,
        completedBy = null,
        completedByDisplayName = null,
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
  fun `Log code must be no more than 10 characters`() {
    val request = CreateCsipRecordRequest(
      logCode = "n".repeat(11),
      referral = CreateReferralRequest(
        incidentDate = LocalDate.now(),
        incidentTime = LocalTime.now(),
        incidentTypeCode = "idque",
        incidentLocationCode = "ridiculus",
        referredBy = "maximus",
        refererAreaCode = "intellegat",
        isProactiveReferral = null,
        isStaffAssaulted = null,
        assaultedStaffName = null,
        incidentInvolvementCode = "vidisse",
        descriptionOfConcern = "molestie",
        knownReasons = "dicat",
        otherInformation = null,
        isSaferCustodyTeamInformed = DO_NOT_KNOW,
        isReferralComplete = null,
        completedDate = null,
        completedBy = null,
        completedByDisplayName = null,
        contributoryFactors = listOf(
          CreateContributoryFactorRequest(
            factorTypeCode = "pericula",
            comment = null,
          ),
        ),
      ),
    )
    assertSingleValidationError(validator.validate(request), "logCode", "Log code must be <= 10 characters")
  }

  @Test
  fun `Log code can be null`() {
    val request = CreateCsipRecordRequest(
      logCode = null,
      referral = CreateReferralRequest(
        incidentDate = LocalDate.now(),
        incidentTime = LocalTime.now(),
        incidentTypeCode = "idque",
        incidentLocationCode = "ridiculus",
        referredBy = "maximus",
        refererAreaCode = "intellegat",
        isProactiveReferral = null,
        isStaffAssaulted = null,
        assaultedStaffName = null,
        incidentInvolvementCode = "vidisse",
        descriptionOfConcern = "molestie",
        knownReasons = "dicat",
        otherInformation = null,
        isSaferCustodyTeamInformed = DO_NOT_KNOW,
        isReferralComplete = null,
        completedDate = null,
        completedBy = null,
        completedByDisplayName = null,
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
  fun `child nested object validation`() {
    val request = CreateCsipRecordRequest(
      logCode = "na",
      referral = CreateReferralRequest(
        incidentDate = LocalDate.now(),
        incidentTime = LocalTime.now(),
        incidentTypeCode = "idque",
        incidentLocationCode = "ridiculus",
        referredBy = "maximus",
        refererAreaCode = "intellegat",
        isProactiveReferral = null,
        isStaffAssaulted = null,
        assaultedStaffName = null,
        incidentInvolvementCode = "vidisse",
        descriptionOfConcern = "molestie",
        knownReasons = "dicat",
        otherInformation = null,
        isSaferCustodyTeamInformed = DO_NOT_KNOW,
        isReferralComplete = null,
        completedDate = null,
        completedBy = null,
        completedByDisplayName = null,
        contributoryFactors = listOf(
          CreateContributoryFactorRequest(
            factorTypeCode = "n".repeat(13),
            comment = null,
          ),
        ),
      ),
    )
    assertSingleValidationError(
      validator.validate(request),
      "referral.contributoryFactors[0].factorTypeCode",
      "Contributory factor type code must be <= 12 characters",
    )
  }
}
