package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary

object EntityGenerator {
  fun generateCsipRecord(
    personSummary: PersonSummary = prisoner().toPersonSummary(),
    prisonCodeWhenRecorded: String? = null,
    logCode: String? = null,
    legacyId: Long? = null,
  ) = CsipRecord(
    personSummary,
    prisonCodeWhenRecorded,
    logCode,
    legacyId,
  )
}
