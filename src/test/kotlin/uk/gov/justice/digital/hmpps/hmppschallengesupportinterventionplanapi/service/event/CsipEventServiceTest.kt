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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.DomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
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
    val csipEventService = EntityEventService(eventProperties, telemetryClient, domainEventPublisher)
    val csipEvent = csipUpdatedEvent()

    csipEventService.handleEvent(csipEvent)

    val domainEventCaptor = argumentCaptor<HmppsDomainEvent<CsipInformation>>()
    verify(domainEventPublisher).publish(domainEventCaptor.capture())
    with(domainEventCaptor.firstValue) {
      assertThat(eventType).isEqualTo(DomainEventType.CSIP_UPDATED.eventType)
      assertThat(description).isEqualTo(DomainEventType.CSIP_UPDATED.description)
      with(additionalInformation) {
        assertThat(recordUuid).isEqualTo(csipEvent.recordUuid)
        assertThat(source).isEqualTo(csipEvent.source)
        assertThat(affectedComponents).isEqualTo(csipEvent.affectedComponents)
      }
      assertThat(detailUrl).isEqualTo("$baseUrl/csip-records/${csipEvent.recordUuid}")
      assertThat(personReference).isEqualTo(PersonReference.withPrisonNumber(PRISON_NUMBER))
    }
  }

  @Test
  fun `handle event - publish disabled`() {
    val eventProperties = EventProperties(false, baseUrl)
    val csipEventService = EntityEventService(eventProperties, telemetryClient, domainEventPublisher)
    val csipEvent = csipUpdatedEvent()

    csipEventService.handleEvent(csipEvent)

    verify(domainEventPublisher, never()).publish(any<DomainEvent>())
  }

  private fun csipUpdatedEvent() = CsipEvent(
    recordUuid = UUID.randomUUID(),
    prisonNumber = PRISON_NUMBER,
    type = DomainEventType.CSIP_UPDATED,
    occurredAt = LocalDateTime.now(),
    source = Source.NOMIS,
    affectedComponents = setOf(CsipComponent.Referral),
  )
}
