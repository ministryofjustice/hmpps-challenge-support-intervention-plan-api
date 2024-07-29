package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.DO_NOT_KNOW
import java.time.LocalDate

class UpdateCsipRecordRequestTest : RequestValidationTest() {
  @Test
  fun `valid request`() {
    val request = UpdateCsipRecordRequest(
      logCode = "menandri",
      UpdateReferral(
        incidentDate = LocalDate.now(),
        incidentTime = null,
        incidentTypeCode = "wisi",
        incidentLocationCode = "fugit",
        referredBy = "sociosqu",
        refererAreaCode = "eu",
        isProactiveReferral = null,
        isStaffAssaulted = null,
        assaultedStaffName = null,
        incidentInvolvementCode = "aliquid",
        descriptionOfConcern = "novum",
        knownReasons = "gravida",
        otherInformation = null,
        isSaferCustodyTeamInformed = DO_NOT_KNOW,
        isReferralComplete = null,
        completedDate = null,
        completedBy = null,
        completedByDisplayName = null,
      ),
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `valid request with null logCode`() {
    val request = UpdateCsipRecordRequest(
      logCode = null,
      null,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `validation fails if size constraints are not met`() {
    val request = UpdateCsipRecordRequest(
      logCode = "n".repeat(11),
      UpdateReferral(
        incidentDate = LocalDate.now(),
        incidentTime = null,
        incidentTypeCode = "n".repeat(13),
        incidentLocationCode = "n".repeat(13),
        referredBy = "n".repeat(241),
        refererAreaCode = "n".repeat(13),
        isProactiveReferral = null,
        isStaffAssaulted = null,
        assaultedStaffName = "n".repeat(1001),
        incidentInvolvementCode = "n".repeat(13),
        descriptionOfConcern = "molestie",
        knownReasons = "dicat",
        otherInformation = null,
        isSaferCustodyTeamInformed = DO_NOT_KNOW,
        isReferralComplete = true,
        completedDate = LocalDate.now(),
        completedBy = "n".repeat(33),
        completedByDisplayName = "n".repeat(256),
      ),
    )
    assertValidationErrors(
      validator.validate(request),
      Pair("logCode", "Log code must be <= 10 characters"),
      Pair("referral.incidentLocationCode", "Incident Location code must be <= 12 characters"),
      Pair("referral.incidentTypeCode", "Incident Type code must be <= 12 characters"),
      Pair("referral.referredBy", "Referer name must be <= 240 characters"),
      Pair("referral.refererAreaCode", "Area code must be <= 12 characters"),
      Pair("referral.incidentInvolvementCode", "Involvement code must be <= 12 characters"),
      Pair("referral.assaultedStaffName", "Name or names must be <= 1000 characters"),
      Pair("referral.completedBy", "Completed by username must be <= 32 characters"),
      Pair("referral.completedByDisplayName", "Completed by display name must be <= 255 characters"),
    )
  }

  @Test
  fun `valid request with incident fields null`() {
    val request = UpdateCsipRecordRequest(
      null,
      UpdateReferral(
        incidentDate = LocalDate.now(),
        incidentTime = null,
        incidentTypeCode = "wisi",
        incidentLocationCode = "fugit",
        referredBy = "sociosqu",
        refererAreaCode = "eu",
        isProactiveReferral = null,
        isStaffAssaulted = null,
        assaultedStaffName = null,
        incidentInvolvementCode = null,
        descriptionOfConcern = null,
        knownReasons = null,
        otherInformation = null,
        isSaferCustodyTeamInformed = DO_NOT_KNOW,
        isReferralComplete = null,
        completedDate = null,
        completedBy = null,
        completedByDisplayName = null,
      ),
    )
    assertThat(validator.validate(request)).isEmpty()
  }
}
