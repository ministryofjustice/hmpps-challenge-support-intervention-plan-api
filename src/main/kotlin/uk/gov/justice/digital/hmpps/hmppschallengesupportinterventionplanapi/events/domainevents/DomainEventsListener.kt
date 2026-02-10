package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

@Service
class DomainEventsListener(
  private val jsonMapper: JsonMapper,
  private val personUpdatedHandler: PersonUpdatedHandler,
  private val moveEventHandler: MoveEventHandler,
  private val reconciliationHandler: PersonReconciliationHandler,
) {
  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    when (notification.eventType) {
      PRISONER_UPDATED -> personUpdatedHandler.handle(jsonMapper.readValue(notification.message))
      PRISONER_MERGED -> moveEventHandler.handleMerge(jsonMapper.readValue(notification.message))
      BOOKING_MOVED -> moveEventHandler.handleBookingMoved(jsonMapper.readValue(notification.message))
      PERSON_RECONCILIATION -> reconciliationHandler.handle(jsonMapper.readValue(notification.message))
    }
  }

  companion object {
    const val PRISONER_UPDATED = "prisoner-offender-search.prisoner.updated"
    const val PRISONER_MERGED = "prison-offender-events.prisoner.merged"
    const val BOOKING_MOVED = "prison-offender-events.prisoner.booking.moved"
    const val PERSON_RECONCILIATION = "csip.person.reconciliation"
  }
}
