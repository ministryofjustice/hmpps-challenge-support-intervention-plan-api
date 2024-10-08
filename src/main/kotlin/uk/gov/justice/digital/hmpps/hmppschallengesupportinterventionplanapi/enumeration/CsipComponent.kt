package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.SaferCustodyScreeningOutcome
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
