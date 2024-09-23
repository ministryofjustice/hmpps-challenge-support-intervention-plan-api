package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.event

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipBaseEvent

@Service
class EntityEventService(
  private val eventProperties: EventProperties,
  private val telemetryClient: TelemetryClient,
  private val domainEventPublisher: DomainEventPublisher,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleEvent(event: CsipBaseEvent) {
    val domainEvent = event.toDomainEvent(eventProperties.baseUrl)
    if (eventProperties.publish) {
      domainEventPublisher.publish(domainEvent)
    } else {
      telemetryClient.trackEvent(
        event.type.eventType,
        listOfNotNull(
          domainEvent.personReference?.findNomsNumber()?.let { "prisonNumber" to it },
          "recordUuid" to event.recordUuid.toString(),
          "detailPath" to event.detailPath(),
          "occurredAt" to event.occurredAt.toString(),
        ).toMap(),
        mapOf(),
      )
    }
  }
}
