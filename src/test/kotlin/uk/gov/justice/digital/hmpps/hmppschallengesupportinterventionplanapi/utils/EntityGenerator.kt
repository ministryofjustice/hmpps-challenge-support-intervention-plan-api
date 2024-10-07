package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.PersonLocation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.toPersonLocation
import java.time.LocalDateTime

object EntityGenerator {
  private val CONTEXT = csipRequestContext()

  fun generateCsipRecord(
    personLocation: PersonLocation = prisoner().toPersonLocation(),
    prisonCodeWhenRecorded: String? = null,
    logCode: String? = null,
    createdAt: LocalDateTime = CONTEXT.requestAt,
    createdBy: String = CONTEXT.username,
    createdByDisplayName: String = CONTEXT.userDisplayName,
    legacyId: Long? = null,
  ) = CsipRecord(
    personLocation,
    prisonCodeWhenRecorded,
    logCode,
    legacyId,
  ).apply {
    this.createdAt = createdAt
    this.createdBy = createdBy
    this.createdByDisplayName = createdByDisplayName
  }
}
