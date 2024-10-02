package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.SYSTEM_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.SYSTEM_USER_NAME
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

class DefaultLegacyActioned(
  override val actionedAt: LocalDateTime,
  actionedBy: String?,
  actionedByDisplayName: String?,
  override val activeCaseloadId: String?,
) : LegacyActioned {
  @Schema(requiredMode = NOT_REQUIRED)
  override val actionedBy: String = actionedBy ?: SYSTEM_USER_NAME
  @Schema(requiredMode = NOT_REQUIRED)
  override val actionedByDisplayName: String = actionedByDisplayName ?: SYSTEM_DISPLAY_NAME
}
