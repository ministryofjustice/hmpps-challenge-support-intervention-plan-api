package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.SaferCustodyScreeningOutcome
import kotlin.reflect.KClass

enum class CsipComponent(val clazz: KClass<*>) {
  RECORD(CsipRecord::class),
  REFERRAL(Referral::class),
  CONTRIBUTORY_FACTOR(ContributoryFactor::class),
  SAFER_CUSTODY_SCREENING_OUTCOME(SaferCustodyScreeningOutcome::class),
  INVESTIGATION(Investigation::class),
  INTERVIEW(Interview::class),
  DECISION_AND_ACTIONS(DecisionAndActions::class),
  PLAN(Plan::class),
  IDENTIFIED_NEED(IdentifiedNeed::class),
  REVIEW(Review::class),
  ATTENDEE(Attendee::class),
  ;

  companion object {
    @JvmStatic
    fun fromClass(javaClass: Class<*>): CsipComponent? = entries.find { it.clazz.java == javaClass }
  }
}
