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
  // Will be used for tracking events for metrics
  private val telemetryClient: TelemetryClient,
  private val domainEventPublisher: DomainEventPublisher,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleEvent(event: T) {
    log.info(event.toString())

    event.toDomainEvent(eventProperties.baseUrl).run {
      if (eventProperties.publish) {
        domainEventPublisher.publish(this)
      } else {
        log.info("$eventType event publishing is disabled")
      }
    }
    // TODO: Track event for metrics
  }

  protected companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
