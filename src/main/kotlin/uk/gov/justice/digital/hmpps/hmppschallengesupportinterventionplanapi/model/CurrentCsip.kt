package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.temporal.ChronoUnit.DAYS

data class CurrentCsipDetail(
  val currentCsip: CurrentCsip?,
  val totalOpenedCsipCount: Int,
  val totalReferralCount: Int,
) {
  companion object {
    val NONE = CurrentCsipDetail(null, 0, 0)
  }
}

data class CurrentCsip(
  val status: ReferenceData,
  val referralDate: LocalDate?,
  val nextReviewDate: LocalDate?,
  val closedDate: LocalDate?,
) {
  val reviewOverdueDays: Long? = nextReviewDate?.let { if (now().isAfter(it)) DAYS.between(it, now()) else null }
}
