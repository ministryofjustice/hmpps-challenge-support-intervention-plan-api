package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext

class AuditedEntityListener {
  @PrePersist
  fun onPrePersist(auditable: Auditable) {
    auditable.recordCreatedDetails(csipRequestContext())
  }

  @PreUpdate
  fun onPreUpdate(auditable: Auditable) {
    auditable.recordModifiedDetails(csipRequestContext())
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
