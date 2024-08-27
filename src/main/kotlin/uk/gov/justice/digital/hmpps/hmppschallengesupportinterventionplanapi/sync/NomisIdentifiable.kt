package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import java.time.LocalDateTime
import java.util.UUID

interface NomisIdentifiable {
  val legacyId: Long
  val id: UUID?
}

abstract class NomisAudited {
  lateinit var createdAt: LocalDateTime
  lateinit var createdBy: String
  lateinit var createdByDisplayName: String
  var lastModifiedAt: LocalDateTime? = null
  var lastModifiedBy: String? = null
  var lastModifiedByDisplayName: String? = null
}

interface LegacyActioned {
  val actionedAt: LocalDateTime
  val actionedBy: String
  val actionedByDisplayName: String
  val activeCaseloadId: String?
}

data class DefaultLegacyActioned(
  override val actionedAt: LocalDateTime,
  override val actionedBy: String,
  override val actionedByDisplayName: String,
  override val activeCaseloadId: String?,
) : LegacyActioned
