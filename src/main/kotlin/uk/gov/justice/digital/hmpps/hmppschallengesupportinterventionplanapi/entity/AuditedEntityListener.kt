package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext

class AuditedEntityListener {
  @PrePersist
  fun onPrePersist(audited: Audited) {
    audited.recordCreatedDetails(csipRequestContext())
  }

  @PreUpdate
  fun onPreUpdate(audited: Audited) {
    audited.recordModifiedDetails(csipRequestContext())
  }
}

class CsipChildEntityListener {
  @PrePersist
  fun onPrePersist(csipChild: CsipChild) {
    csipChild.csipRecord.recordModifiedDetails(csipRequestContext())
  }

  @PreUpdate
  fun onPreUpdate(csipChild: CsipChild) {
    csipChild.csipRecord.recordModifiedDetails(csipRequestContext())
  }
}
