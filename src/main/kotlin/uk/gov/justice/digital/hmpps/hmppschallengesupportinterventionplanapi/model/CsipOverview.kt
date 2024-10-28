package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

data class CsipOverview(val counts: CsipCounts)
data class CsipCounts(
  val submittedReferrals: Int,
  val pendingInvestigations: Int,
  val awaitingDecisions: Int,
  val pendingPlans: Int,
  val open: Int,
  val overdueReviews: Int,
) {
  companion object {
    val NONE = CsipCounts(0, 0, 0, 0, 0, 0)
  }
}
