package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import tools.jackson.module.kotlin.jsonMapper
import java.time.ZonedDateTime
import java.util.UUID

class DomainEventsListenerTest {
  private val jsonMapper = jsonMapper()
  private val personUpdatedHandler = mock<PersonUpdatedHandler>()
  private val moveEventHandler = mock<MoveEventHandler>()
  private val reconciliationHandler = mock<PersonReconciliationHandler>()
  private val personCaseNoteHandler = mock<PersonCaseNoteHandler>()
  private val listener = DomainEventsListener(
    jsonMapper,
    personUpdatedHandler,
    moveEventHandler,
    reconciliationHandler,
    personCaseNoteHandler,
  )

  @Test
  fun `deleted case note event is routed to person case note handler`() {
    val caseNoteId = UUID.randomUUID()
    val event = HmppsDomainEvent(
      occurredAt = ZonedDateTime.now(),
      eventType = DomainEventsListener.PERSON_CASE_NOTE_DELETED,
      detailUrl = null,
      description = "A case note has been deleted",
      additionalInformation = PersonCaseNoteInformation(id = caseNoteId),
      personReference = PersonReference.withPrisonNumber("A12345BC"),
    )

    listener.receive(
      Notification(
        message = jsonMapper.writeValueAsString(event),
        attributes = MessageAttributes(DomainEventsListener.PERSON_CASE_NOTE_DELETED),
      ),
    )

    val eventCaptor = argumentCaptor<HmppsDomainEvent<PersonCaseNoteInformation>>()
    verify(personCaseNoteHandler).handle(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("person.case-note.deleted")
      assertThat(additionalInformation.id).isEqualTo(caseNoteId)
      assertThat(personReference?.findNomsNumber()).isEqualTo("G0233VH")
    }
  }
}
