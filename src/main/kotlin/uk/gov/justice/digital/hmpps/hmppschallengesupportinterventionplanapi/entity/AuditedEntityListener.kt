package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.PreUpdate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source

class AuditedEntityListener {

  @PreUpdate
  fun onPreUpdate(auditable: Auditable) {
    val context = csipRequestContext()
    if (context.source == Source.NOMIS) return
    auditable.recordModifiedDetails(context)
  }
}
