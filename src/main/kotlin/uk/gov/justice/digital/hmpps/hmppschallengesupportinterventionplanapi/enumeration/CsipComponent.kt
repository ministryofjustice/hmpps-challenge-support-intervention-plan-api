package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import kotlin.reflect.KClass

enum class CsipComponent(val clazz: KClass<*>, val description: String) {
  Record(CsipRecord::class, "person.csip.record"),
  Referral(uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral::class, ""),
  ContributoryFactor(
    uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor::class,
    "person.csip.contributory-factor",
  ),
  SaferCustodyScreeningOutcome(
    uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.SaferCustodyScreeningOutcome::class,
    "",
  ),
  Investigation(uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation::class, ""),
  Interview(
    uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Interview::class,
    "person.csip.interview",
  ),
  DecisionAndActions(
    uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.DecisionAndActions::class,
    "",
  ),
  Plan(uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan::class, ""),
  IdentifiedNeed(
    uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.IdentifiedNeed::class,
    "person.csip.identified-need",
  ),
  Review(
    uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review::class,
    "person.csip.review",
  ),
  Attendee(
    uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Attendee::class,
    "person.csip.attendee",
  ),
  ;

  companion object {
    @JvmStatic
    fun fromClass(javaClass: Class<*>): CsipComponent? = entries.find { it.clazz.java == javaClass }
  }
}
