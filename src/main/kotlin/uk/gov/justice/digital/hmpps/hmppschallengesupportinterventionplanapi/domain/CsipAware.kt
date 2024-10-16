package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

interface CsipAware {
  fun csipRecord(): CsipRecord
}
