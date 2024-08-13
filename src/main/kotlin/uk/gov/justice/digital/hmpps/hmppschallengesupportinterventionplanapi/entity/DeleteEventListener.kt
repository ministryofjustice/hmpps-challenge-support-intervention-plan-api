package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.PreRemove
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.EventFactory.csipChildEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.EventFactory.csipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.Companion.fromClass
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.PersistenceAction

@Component
class DeleteEventListener {

  private lateinit var publisher: ApplicationEventPublisher

  @Autowired
  fun setPublisher(@Lazy publisher: ApplicationEventPublisher) {
    this.publisher = publisher
  }

  @PreRemove
  fun onPreRemove(identifiable: Identifiable) {
    val component = fromClass(identifiable::class.java)
    if (identifiable is CsipRecord) {
      publisher.publishEvent(
        csipEvent(
          identifiable.prisonNumber,
          PersistenceAction.DELETED,
          identifiable.id,
          identifiable.components(),
        ),
      )
    }
    if (identifiable is CsipAware && component != null) {
      val record = identifiable.csipRecord()
      publisher.publishEvent(
        csipChildEvent(
          record.prisonNumber,
          component,
          PersistenceAction.DELETED,
          record.id,
          identifiable.id,
        ),
      )
    }
  }
}
