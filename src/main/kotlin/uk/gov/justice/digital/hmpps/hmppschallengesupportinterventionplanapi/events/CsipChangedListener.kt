package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events

import jakarta.persistence.PrePersist
import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.Versioned

@Component
class CsipChangedListener {

  private lateinit var publisher: EventPublisher

  @Autowired
  fun setPublisher(@Lazy publisher: EventPublisher) {
    this.publisher = publisher
  }

  @PrePersist
  fun onPrePersist(auditable: Versioned) {
    auditable.publishEvent()
  }

  @PreUpdate
  fun onPreUpdate(auditable: Versioned) {
    auditable.publishEvent()
  }

  @PreRemove
  fun onPreRemove(auditable: Versioned) {
    auditable.publishEvent()
  }

  private fun Versioned.publishEvent() {
    when (this) {
      is CsipRecord -> this
      is CsipAware -> csipRecord()
      else -> null
    }?.also {
      publisher.csipEvent(
        it.prisonNumber,
        it.id,
      )
    }
  }
}
