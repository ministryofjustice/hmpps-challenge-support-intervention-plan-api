package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

enum class AffectedComponent : Comparable<AffectedComponent> {
  Record,
  Referral,
  ContributoryFactor,
  SaferCustodyScreeningOutcome,
  Investigation,
  Interview,
  DecisionAndActions,
  Plan,
  IdentifiedNeed,
  Review,
  Attendee,
}
