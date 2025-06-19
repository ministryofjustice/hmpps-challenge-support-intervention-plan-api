package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service

@Service
class DomainEventsListener(
  private val objectMapper: ObjectMapper,
  private val personUpdatedHandler: PersonUpdatedHandler,
  private val moveEventHandler: MoveEventHandler,
) {
  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    when (notification.eventType) {
      PRISONER_UPDATED -> personUpdatedHandler.handle(objectMapper.readValue(notification.message))
      PRISONER_MERGED -> moveEventHandler.handleMerge(objectMapper.readValue(notification.message))
      BOOKING_MOVED -> moveEventHandler.handleBookingMoved(objectMapper.readValue(notification.message))
    }
  }

  companion object {
    const val PRISONER_UPDATED = "prisoner-offender-search.prisoner.updated"
    const val PRISONER_MERGED = "prison-offender-events.prisoner.merged"
    const val BOOKING_MOVED = "prison-offender-events.prisoner.booking.moved"
  }
}
