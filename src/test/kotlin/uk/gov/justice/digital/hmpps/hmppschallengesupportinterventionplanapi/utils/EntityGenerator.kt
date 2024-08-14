package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import java.time.LocalDateTime

object EntityGenerator {

  fun generateCsipRecord(
    prisonNumber: String,
    prisonCodeWhenRecorded: String? = null,
    logCode: String? = null,
    createdAt: LocalDateTime = LocalDateTime.now().minusDays(1),
    createdBy: String = "createdBy",
    createdByDisplayName: String = "createdByDisplayName",
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
