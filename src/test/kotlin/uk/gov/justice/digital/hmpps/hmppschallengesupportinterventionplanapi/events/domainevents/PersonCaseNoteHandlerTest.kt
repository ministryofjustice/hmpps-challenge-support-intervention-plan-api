package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.mock
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import java.time.ZonedDateTime
import java.util.UUID

@ExtendWith(OutputCaptureExtension::class)
class PersonCaseNoteHandlerTest {
  private val personCaseNoteInvestigationService = mock<PersonCaseNoteInvestigationService>()
  private val handler = PersonCaseNoteHandler(personCaseNoteInvestigationService)

  @Test
  fun `created event with valid prison number resolves investigations and logs receipt`(output: CapturedOutput) {
    val prisonNumber = "A1234BC"
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonNumber))
      .thenReturn(PersonCaseNoteInvestigationResolution(prisonNumber, emptyList(), emptyList()))

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonNumber))

    verify(personCaseNoteInvestigationService, times(1)).resolveInvestigations(prisonNumber)
    assertThat(output.out).contains("Received person.case-note.created event")
    assertThat(output.out).contains("Resolved prison number A1234BC")
  }

  @Test
  fun `updated event with valid prison number resolves investigations and logs receipt`(output: CapturedOutput) {
    val prisonNumber = "A1234BC"
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonNumber))
      .thenReturn(PersonCaseNoteInvestigationResolution(prisonNumber, emptyList(), emptyList()))

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_UPDATED, prisonNumber = prisonNumber))

    verify(personCaseNoteInvestigationService, times(1)).resolveInvestigations(prisonNumber)
    assertThat(output.out).contains("Received person.case-note.updated event")
    assertThat(output.out).contains("Resolved prison number A1234BC")
  }

  @Test
  fun `missing personReference logs warning and exits`(output: CapturedOutput) {
    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, personReference = null))

    verify(personCaseNoteInvestigationService, never()).resolveInvestigations(org.mockito.kotlin.any())
    assertThat(output.out).contains("because no NOMS prison number was provided in personReference")
  }

  @Test
  fun `missing NOMS identifier logs warning and exits`(output: CapturedOutput) {
    handler.handle(
      caseNoteEvent(
        DomainEventsListener.PERSON_CASE_NOTE_CREATED,
        personReference = PersonReference(
          identifiers = listOf(PersonReference.Identifier(type = "PNC", value = "123")),
        ),
      ),
    )

    verify(personCaseNoteInvestigationService, never()).resolveInvestigations(org.mockito.kotlin.any())
    assertThat(output.out).contains("because no NOMS prison number was provided in personReference")
  }

  private fun caseNoteEvent(
    eventType: String,
    prisonNumber: String = "A1234BC",
    personReference: PersonReference? = PersonReference.withPrisonNumber(prisonNumber),
  ) = HmppsDomainEvent(
    occurredAt = ZonedDateTime.now(),
    eventType = eventType,
    detailUrl = null,
    description = "Case note changed",
    additionalInformation = PersonCaseNoteInformation(
      id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      type = "OBSERVE",
      subType = "GEN",
    ),
    personReference = personReference,
  )
}

