package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.event

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

class CsipEventServiceTest {
  private val telemetryClient = mock<TelemetryClient>()
  private val domainEventPublisher = mock<DomainEventPublisher>()

  private val baseUrl = "http://localhost:8080"

  @Test
  fun `handle event - publish enabled`() {
    val eventProperties = EventProperties(true, baseUrl)
    val csipEventService = EntityEventService<CsipEvent>(eventProperties, telemetryClient, domainEventPublisher)
    val csipEvent = csipUpdatedEvent()

    csipEventService.handleEvent(csipEvent)

    val domainEventCaptor = argumentCaptor<CsipDomainEvent>()
    verify(domainEventPublisher).publish(domainEventCaptor.capture())
    assertThat(domainEventCaptor.firstValue).isEqualTo(
      CsipDomainEvent(
        eventType = DomainEventType.CSIP_UPDATED.eventType,
        additionalInformation = CsipAdditionalInformation(
          recordUuid = csipEvent.recordUuid,
          source = csipEvent.source,
          affectedComponents = csipEvent.affectedComponents,
        ),
        description = DomainEventType.CSIP_UPDATED.description,
        occurredAt = csipEvent.occurredAt.toZoneDateTime(),
        detailUrl = "$baseUrl/csip-records/${csipEvent.recordUuid}",
        personReference = PersonReference.withPrisonNumber(PRISON_NUMBER),
      ),
    )
  }

  @Test
  fun `handle event - publish disabled`() {
    val eventProperties = EventProperties(false, baseUrl)
    val csipEventService = EntityEventService<CsipEvent>(eventProperties, telemetryClient, domainEventPublisher)
    val csipEvent = csipUpdatedEvent()

    csipEventService.handleEvent(csipEvent)

    verify(domainEventPublisher, never()).publish(any<CsipDomainEvent>())
  }

  private fun csipUpdatedEvent() = CsipUpdatedEvent(
    recordUuid = UUID.randomUUID(),
    prisonNumber = PRISON_NUMBER,
    description = DomainEventType.CSIP_UPDATED.description,
    occurredAt = LocalDateTime.now(),
    source = Source.NOMIS,
    affectedComponents = setOf(AffectedComponent.Referral),
  )
}
