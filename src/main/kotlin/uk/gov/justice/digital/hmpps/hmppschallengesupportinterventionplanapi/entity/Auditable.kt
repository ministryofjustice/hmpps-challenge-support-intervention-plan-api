package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import java.time.LocalDateTime

interface Versioned {
  val version: Int?
}

interface Auditable {
  var createdAt: LocalDateTime
  var createdBy: String
  var createdByDisplayName: String

  var lastModifiedAt: LocalDateTime?
  var lastModifiedBy: String?
  var lastModifiedByDisplayName: String?

  fun recordCreatedDetails(context: CsipRequestContext) {
    createdAt = context.requestAt
    createdBy = context.username
    createdByDisplayName = context.userDisplayName
  }

  fun recordModifiedDetails(context: CsipRequestContext) {
    lastModifiedAt = context.requestAt
    lastModifiedBy = context.username
    lastModifiedByDisplayName = context.userDisplayName
  }
}

@Audited(withModifiedFlag = false)
@MappedSuperclass
open class SimpleAuditable(context: CsipRequestContext = csipRequestContext()) : Auditable, Versioned {
  @field:NotAudited
  @field:Version
  override val version: Int? = null

  @field:Column
  override var createdAt: LocalDateTime = context.requestAt

  @field:Column
  override var createdBy: String = context.username

  @field:Column
  override var createdByDisplayName: String = context.userDisplayName

  @field:Column
  override var lastModifiedAt: LocalDateTime? = null

  @field:Column
  override var lastModifiedBy: String? = null

  @field:Column
  override var lastModifiedByDisplayName: String? = null
}
