package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import java.time.LocalDateTime

object EntityGenerator {
  private val CONTEXT = csipRequestContext()

  fun generateCsipRecord(
    personSummary: PersonSummary = prisoner().toPersonSummary(),
    prisonCodeWhenRecorded: String? = null,
    logCode: String? = null,
    createdAt: LocalDateTime = CONTEXT.requestAt,
    legacyId: Long? = null,
  ) = CsipRecord(
    personSummary,
    prisonCodeWhenRecorded,
    logCode,
    legacyId,
  ).apply {
    set(::createdAt, createdAt)
  }
}
