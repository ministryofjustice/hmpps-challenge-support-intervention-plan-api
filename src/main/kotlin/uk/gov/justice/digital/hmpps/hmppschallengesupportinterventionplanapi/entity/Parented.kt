package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

interface Parented {
  fun parent(): Auditable
}
