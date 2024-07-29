package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.PostPersist
import jakarta.persistence.PrePersist
import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction

@Component
class AuditedEntityListener {

  private lateinit var auditEventRepository: AuditEventRepository

  @Autowired
  fun setAuditEventRepository(@Lazy auditEventRepository: AuditEventRepository) {
    this.auditEventRepository = auditEventRepository
  }

  @PrePersist
  fun onPrePersist(auditable: Auditable) {
    auditable.recordCreatedDetails(csipRequestContext())
  }

  @PostPersist
  fun onPostPersist(auditable: Auditable) {
    updateAuditEvents(auditable)
  }

  @PreUpdate
  fun onPreUpdate(auditable: Auditable) {
    updateAuditEvents(auditable)
    auditable.recordModifiedDetails(csipRequestContext())
  }

  @PreRemove
  fun onPreRemove(auditable: Auditable) {
    updateAuditEvents(auditable)
    auditable.recordModifiedDetails(csipRequestContext())
  }

  private fun updateAuditEvents(auditable: Auditable) {
    if (auditable is CsipRecord) {
      auditable.auditEvents?.map { it.asEntity(auditable.id) }?.forEach(auditEventRepository::save)
      auditable.auditEvents = mutableSetOf()
    }
  }
}

class UpdateParentEntityListener {
  @PrePersist
  fun onPrePersist(parented: Parented) {
    parented.parent().recordModifiedDetails(csipRequestContext())
  }

  @PreUpdate
  fun onPreUpdate(parented: Parented) {
    parented.parent().recordModifiedDetails(csipRequestContext())
  }
}

data class AuditRequest(
  val action: AuditEventAction,
  val description: String,
  val affectedComponents: Set<AffectedComponent>,
)

fun AuditRequest.asEntity(csipRecordId: Long): AuditEvent {
  val context = csipRequestContext()
  return AuditEvent(
    csipRecordId = csipRecordId,
    action = action,
    description = description,
    actionedAt = context.requestAt,
    actionedBy = context.username,
    actionedByCapturedName = context.userDisplayName,
    source = context.source,
    activeCaseLoadId = context.activeCaseLoadId,
    affectedComponents = affectedComponents,
  )
}
