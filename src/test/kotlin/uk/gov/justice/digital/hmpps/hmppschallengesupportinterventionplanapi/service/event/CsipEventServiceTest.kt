package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.event

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.ServiceConfig
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.EntityEventService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.DomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.DomainEventPublisher
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import java.time.LocalDateTime
import java.util.UUID

class CsipEventServiceTest {
  private val telemetryClient = mock<TelemetryClient>()
  private val domainEventPublisher = mock<DomainEventPublisher>()

  private val baseUrl = "http://localhost:8080"

  @Test
  fun `handle event - publish enabled`() {
    val serviceConfig = ServiceConfig(setOf(PRISON_CODE_LEEDS), baseUrl, true, 4, 200)
    val csipEventService = EntityEventService(serviceConfig, telemetryClient, domainEventPublisher)
    val csipEvent = csipUpdatedEvent()

    csipEventService.handleEvent(csipEvent)

    val domainEventCaptor = argumentCaptor<HmppsDomainEvent<CsipInformation>>()
    verify(domainEventPublisher).publish(domainEventCaptor.capture())
    with(domainEventCaptor.firstValue) {
      assertThat(eventType).isEqualTo(DomainEventType.CSIP_UPDATED.eventType)
      assertThat(description).isEqualTo(DomainEventType.CSIP_UPDATED.description)
      with(additionalInformation) {
        assertThat(recordUuid).isEqualTo(csipEvent.recordUuid)
      }
      assertThat(detailUrl).isEqualTo("$baseUrl/csip-records/${csipEvent.recordUuid}")
      assertThat(personReference).isEqualTo(PersonReference.withPrisonNumber(PRISON_NUMBER))
    }
  }

  @Test
  fun `handle event - publish disabled`() {
    val serviceConfig = ServiceConfig(setOf(PRISON_CODE_LEEDS), baseUrl, false, 4, 200)
    val csipEventService = EntityEventService(serviceConfig, telemetryClient, domainEventPublisher)
    val csipEvent = csipUpdatedEvent()

    csipEventService.handleEvent(csipEvent)

    verify(domainEventPublisher, never()).publish(any<DomainEvent>())
  }

  private fun csipUpdatedEvent() = CsipEvent(
    type = DomainEventType.CSIP_UPDATED,
    prisonNumber = PRISON_NUMBER,
    recordUuid = UUID.randomUUID(),
    occurredAt = LocalDateTime.now(),
  )
}
