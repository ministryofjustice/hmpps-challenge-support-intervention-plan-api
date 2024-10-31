package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.SYSTEM_USER_NAME
import java.time.LocalDateTime
import java.util.UUID

interface NomisIdentifiable {
  val legacyId: Long
  val id: UUID?
}

interface LegacyActioned {
  val actionedAt: LocalDateTime
  val actionedBy: String
  val activeCaseloadId: String?
}

class DefaultLegacyActioned(
  override val actionedAt: LocalDateTime,
  actionedBy: String?,
  override val activeCaseloadId: String?,
) : LegacyActioned {
  @Schema(requiredMode = NOT_REQUIRED)
  override val actionedBy: String = actionedBy ?: SYSTEM_USER_NAME
}
