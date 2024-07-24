package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.PreUpdate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext

class CsipEntityListener {
  @PreUpdate
  fun onPreUpdate(csipRecord: CsipRecord) {
    csipRecord.recordModifiedDetails(csipRequestContext())
  }
}

class CsipChildEntityListener {
  @PreUpdate
  fun onPreUpdate(csipChild: CsipChild) {
    csipChild.csipRecord.recordModifiedDetails(csipRequestContext())
  }
}
