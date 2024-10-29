package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

data class CsipOverview(val counts: CsipCounts)
data class CsipCounts(
  val submittedReferrals: Long,
  val pendingInvestigations: Long,
  val awaitingDecisions: Long,
  val pendingPlans: Long,
  val open: Long,
  val overdueReviews: Long,
) {
  companion object {
    val NONE = CsipCounts(0, 0, 0, 0, 0, 0)
  }
}
