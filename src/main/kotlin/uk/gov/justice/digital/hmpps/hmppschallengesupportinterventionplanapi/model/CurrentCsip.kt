package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import java.time.LocalDate

data class CurrentCsipDetail(val currentCsip: CurrentCsip?, val totalOpenedCsipCount: Int, val totalReferralCount: Int) {
  companion object {
    val NONE = CurrentCsipDetail(null, 0, 0)
  }
}
data class CurrentCsip(val status: CsipStatus, val referralDate: LocalDate, val nextReviewDate: LocalDate?)
