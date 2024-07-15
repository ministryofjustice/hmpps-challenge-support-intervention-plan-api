package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.event

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBaseEvent

@Service
class EntityEventService<T : CsipBaseEvent<*>>(
  private val eventProperties: EventProperties,
  private val telemetryClient: TelemetryClient,
  private val domainEventPublisher: DomainEventPublisher,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleEvent(event: T) {
    if (eventProperties.publish) {
      val domainEvent = event.toDomainEvent(eventProperties.baseUrl)
      domainEventPublisher.publish(domainEvent)
      telemetryClient.trackEvent(
        event.type.eventType,
        listOfNotNull(
          domainEvent.personReference?.findNomsNumber()?.let { "prisonNumber" to it },
          "recordUuid" to event.recordUuid.toString(),
          "detailPath" to event.detailPath(),
          "source" to event.source.toString(),
          "occurredAt" to event.occurredAt.toString(),
        ).toMap(),
        mapOf(),
      )
    } else {
      log.info("${event.type.eventType} publishing is disabled")
    }
  }

  protected companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
