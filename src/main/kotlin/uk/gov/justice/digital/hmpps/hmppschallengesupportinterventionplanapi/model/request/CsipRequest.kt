package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

interface CsipRequest {
  val logCode: String?
  val referral: ReferralRequest?
}
