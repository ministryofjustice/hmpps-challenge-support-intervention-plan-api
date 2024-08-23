package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import java.time.LocalDateTime

object EntityGenerator {
  private val CONTEXT = csipRequestContext()

  fun generateCsipRecord(
    prisonNumber: String,
    prisonCodeWhenRecorded: String? = null,
    logCode: String? = null,
    createdAt: LocalDateTime = CONTEXT.requestAt,
    createdBy: String = CONTEXT.username,
    createdByDisplayName: String = CONTEXT.userDisplayName,
  ) = CsipRecord(
    prisonNumber,
    prisonCodeWhenRecorded,
    logCode,
  ).apply {
    this.createdAt = createdAt
    this.createdBy = createdBy
    this.createdByDisplayName = createdByDisplayName
  }
}
