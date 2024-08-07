package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.PrePersist
import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext

@Component
class AuditedEntityListener {

  @PrePersist
  fun onPrePersist(auditable: Auditable) {
    auditable.recordCreatedDetails(csipRequestContext())
  }

  @PreUpdate
  fun onPreUpdate(auditable: Auditable) {
    auditable.recordModifiedDetails(csipRequestContext())
  }

  @PreRemove
  fun onPreRemove(auditable: Auditable) {
    auditable.recordModifiedDetails(csipRequestContext())
  }
}
