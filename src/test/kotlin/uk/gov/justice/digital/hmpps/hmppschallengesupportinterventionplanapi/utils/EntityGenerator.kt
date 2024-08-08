package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import java.time.LocalDateTime
import java.util.UUID

object EntityGenerator {

  fun generateCsipRecord(
    prisonNumber: String,
    prisonCodeWhenRecorded: String? = null,
    logCode: String? = null,
    createdAt: LocalDateTime = LocalDateTime.now().minusDays(1),
    createdBy: String = "createdBy",
    createdByDisplayName: String = "createdByDisplayName",
    uuid: UUID = UUID.randomUUID(),
    id: Long = IdGenerator.newId(),
  ) = CsipRecord(
    prisonNumber,
    prisonCodeWhenRecorded,
    logCode,
    uuid,
    id,
  ).apply {
    this.createdAt = createdAt
    this.createdBy = createdBy
    this.createdByDisplayName = createdByDisplayName
  }
}
