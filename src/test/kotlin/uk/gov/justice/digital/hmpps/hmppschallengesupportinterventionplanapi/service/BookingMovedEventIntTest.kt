package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_MOVED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.BookingMovedInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.DomainEventsListener.Companion.BOOKING_MOVED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.NomisIdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import java.time.Duration.ofSeconds
import java.time.ZonedDateTime

class BookingMovedEventIntTest : IntegrationTestBase() {
  @Test
  fun `message ignored if prison number not of interest`() {
    val oldNoms = prisonNumber()
    val newNoms = prisonNumber()

    sendDomainEvent(bookingMovedEvent(oldNoms, newNoms))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    assertThat(personSummaryRepository.findByIdOrNull(oldNoms)).isNull()
    assertThat(personSummaryRepository.findByIdOrNull(newNoms)).isNull()
  }

  @Test
  fun `message ignored if no csip on old noms number`() {
    val oldNoms = prisonNumber()
    val newNoms = prisonNumber()
    assertThat(personSummaryRepository.findByIdOrNull(oldNoms)).isNull()

    val newPerson = prisoner(newNoms).toPersonSummary()
    dataSetup(generateCsipRecord(newPerson)) { it.withCompletedReferral() }
    dataSetup(generateCsipRecord(newPerson)) { it.withCompletedReferral() }

    sendDomainEvent(bookingMovedEvent(oldNoms, newNoms))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    assertThat(personSummaryRepository.findByIdOrNull(oldNoms)).isNull()
  }

  @Test
  fun `person summary removed if no csip records to move`() {
    val oldNoms = prisonNumber()
    val newNoms = prisonNumber()
    assertThat(personSummaryRepository.findByIdOrNull(newNoms)).isNull()
    personSummaryRepository.save(prisoner(oldNoms).toPersonSummary())

    sendDomainEvent(bookingMovedEvent(oldNoms, newNoms))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    assertThat(personSummaryRepository.findByIdOrNull(newNoms)).isNull()
    assertThat(personSummaryRepository.findByIdOrNull(oldNoms)).isNull()
  }

  @Test
  fun `csip records moved to new prison number on booking moved event`() {
    val oldNoms = prisonNumber()
    val newNoms = prisonNumber()
    val oldPerson = prisoner(oldNoms).toPersonSummary()

    val csip1 = dataSetup(generateCsipRecord(oldPerson)) { it.withCompletedReferral() }
    val csip2 = dataSetup(generateCsipRecord(oldPerson)) { it.withCompletedReferral() }

    dataSetup(generateCsipRecord(prisoner(newNoms).toPersonSummary())) { it.withCompletedReferral() }

    sendDomainEvent(bookingMovedEvent(oldNoms, newNoms))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    assertThat(csipRecordRepository.countByPrisonNumber(newNoms)).isEqualTo(3)
    assertThat(personSummaryRepository.findByIdOrNull(oldNoms)).isNull()

    verifyDomainEvents(newNoms, setOf(csip1.id, csip2.id), CSIP_MOVED, 2, Source.NOMIS, oldNoms)
  }

  @Test
  fun `person record created and csip records moved to new prison number on booking moved event`() {
    val oldNoms = prisonNumber()
    val newNoms = prisonNumber()
    givenValidPrisonNumber(newNoms)
    assertThat(personSummaryRepository.findByIdOrNull(newNoms)).isNull()

    val oldPerson = prisoner(oldNoms).toPersonSummary()
    val csip1 = dataSetup(generateCsipRecord(oldPerson)) { it.withCompletedReferral() }
    val csip2 = dataSetup(generateCsipRecord(oldPerson)) { it.withCompletedReferral() }

    sendDomainEvent(bookingMovedEvent(oldNoms, newNoms))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    assertThat(csipRecordRepository.countByPrisonNumber(newNoms)).isEqualTo(2)
    assertThat(personSummaryRepository.findByIdOrNull(oldNoms)).isNull()

    verifyDomainEvents(newNoms, setOf(csip1.id, csip2.id), CSIP_MOVED, 2, Source.NOMIS, oldNoms)
  }

  private fun bookingMovedEvent(
    previousNomsNumber: String,
    newNomsNumber: String,
    datetime: ZonedDateTime = ZonedDateTime.now().minusMinutes(30),
  ): HmppsDomainEvent<BookingMovedInformation> = HmppsDomainEvent(
    occurredAt = ZonedDateTime.now().plusHours(1),
    eventType = BOOKING_MOVED,
    detailUrl = null,
    description = "a NOMIS booking has moved between prisoners",
    BookingMovedInformation(previousNomsNumber, newNomsNumber, datetime),
    PersonReference.withPrisonNumber(newNomsNumber),
  )
}
