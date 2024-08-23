package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Auditable
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

fun <T : Auditable> T.withAuditInfo(request: NomisAudited): T = apply {
  createdAt = request.createdAt
  createdBy = request.createdBy
  createdByDisplayName = request.createdByDisplayName
  lastModifiedAt = request.lastModifiedAt
  lastModifiedBy = request.lastModifiedBy
  lastModifiedByDisplayName = request.lastModifiedByDisplayName
}
