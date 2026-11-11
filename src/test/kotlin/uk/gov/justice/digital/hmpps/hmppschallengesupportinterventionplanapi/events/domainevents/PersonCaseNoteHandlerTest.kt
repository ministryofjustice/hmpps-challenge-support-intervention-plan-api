package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JustifyingSpan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNoteAnnotationsService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNotesService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.JdaService
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

@ExtendWith(OutputCaptureExtension::class)
class PersonCaseNoteHandlerTest {
  private val personCaseNoteInvestigationService = mock<PersonCaseNoteInvestigationService>()
  private val jdaService = mock<JdaService>()
  private val caseNoteAnnotationsService = mock<CaseNoteAnnotationsService>()
  private val caseNoteAnalysedRepository = mock<CaseNoteAnalysedRepository>()
  private val caseNotesService = mock<CaseNotesService>()
  private val handler = PersonCaseNoteHandler(
    personCaseNoteInvestigationService,
    jdaService,
    caseNoteAnnotationsService,
    caseNoteAnalysedRepository,
    caseNotesService,
  )

  init {
    whenever(caseNoteAnalysedRepository.existsByInvestigationId(any())).thenReturn(true)
    whenever(caseNotesService.getCaseNote(any(), any())).thenReturn(testCaseNote())
  }

  @Test
  fun `created event with valid prisoner number resolves investigations and logs receipt`(output: CapturedOutput) {
    val prisonerNumber = "A1234BC"
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonerNumber))
      .thenReturn(PersonCaseNoteInvestigationResolution(prisonerNumber, emptyList(), emptyList()))

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonerNumber))

    verify(personCaseNoteInvestigationService, times(1)).resolveInvestigations(prisonerNumber)
    verify(jdaService, never()).submitCaseNotesForReAnalysis(any(), any(), any())
    assertThat(output.out).contains("Received person.case-note.created event")
    assertThat(output.out).contains("Resolved prisoner number A1234BC")
    assertThat(output.out).contains(
      "Skipping case note analysis for caseNoteId=11111111-1111-1111-1111-111111111111, prisonerNumber=A1234BC, eventType=person.case-note.created because no eligible investigations were found",
    )
  }

  @Test
  fun `updated event with valid prisoner number resolves investigations and logs receipt`(output: CapturedOutput) {
    val prisonerNumber = "A1234BC"
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonerNumber))
      .thenReturn(PersonCaseNoteInvestigationResolution(prisonerNumber, emptyList(), emptyList()))

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_UPDATED, prisonNumber = prisonerNumber))

    verify(personCaseNoteInvestigationService, times(1)).resolveInvestigations(prisonerNumber)
    verify(jdaService, never()).submitCaseNotesForReAnalysis(any(), any(), any())
    assertThat(output.out).contains("Received person.case-note.updated event")
    assertThat(output.out).contains("Resolved prisoner number A1234BC")
    assertThat(output.out).contains(
      "Skipping case note analysis for caseNoteId=11111111-1111-1111-1111-111111111111, prisonerNumber=A1234BC, eventType=person.case-note.updated because no eligible investigations were found",
    )
  }

  @Test
  fun `eligible investigations trigger re-analysis submissions`() {
    val prisonerNumber = "A1234BC"
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val investigationId1 = UUID.randomUUID()
    val investigationId2 = UUID.randomUUID()
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonerNumber))
      .thenReturn(
        PersonCaseNoteInvestigationResolution(
          prisonerNumber,
          listOf(investigationId1, investigationId2),
          listOf(
            InvestigationEvaluation(investigationId1, "INVESTIGATION_PENDING", true),
            InvestigationEvaluation(investigationId2, "INVESTIGATION_PENDING", true),
          ),
        ),
      )

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonerNumber))

    verify(jdaService, times(1)).submitCaseNotesForReAnalysis(prisonerNumber, investigationId1, caseNoteId)
    verify(jdaService, times(1)).submitCaseNotesForReAnalysis(prisonerNumber, investigationId2, caseNoteId)
  }

  @Test
  fun `eligible investigations persist synchronous annotations and log completed update`(output: CapturedOutput) {
    val prisonerNumber = "A1234BC"
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val investigationId = UUID.randomUUID()
    val response = testJdaRequestResponse(investigationId, caseNoteId)
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonerNumber))
      .thenReturn(
        PersonCaseNoteInvestigationResolution(
          prisonerNumber,
          listOf(investigationId),
          listOf(InvestigationEvaluation(investigationId, "INVESTIGATION_PENDING", true)),
        ),
      )
    whenever(jdaService.submitCaseNotesForReAnalysis(prisonerNumber, investigationId, caseNoteId)).thenReturn(response)

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonerNumber))

    verify(jdaService).submitCaseNotesForReAnalysis(prisonerNumber, investigationId, caseNoteId)
    verify(caseNoteAnnotationsService).persistSynchronousAnnotations(response, prisonerNumber)
    assertThat(output.out).contains(
      "Suggested case notes updated for investigationId=$investigationId, requestId=${response.requestId}, caseNoteId=$caseNoteId",
    )
  }

  @Test
  fun `eligible investigations do not persist annotations when re-analysis returns null`() {
    val prisonerNumber = "A1234BC"
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val investigationId = UUID.randomUUID()
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonerNumber))
      .thenReturn(
        PersonCaseNoteInvestigationResolution(
          prisonerNumber,
          listOf(investigationId),
          listOf(InvestigationEvaluation(investigationId, "INVESTIGATION_PENDING", true)),
        ),
      )
    whenever(jdaService.submitCaseNotesForReAnalysis(prisonerNumber, investigationId, caseNoteId)).thenReturn(null)

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonerNumber))

    verify(jdaService).submitCaseNotesForReAnalysis(prisonerNumber, investigationId, caseNoteId)
    verify(caseNoteAnnotationsService, never()).persistSynchronousAnnotations(any(), any())
  }

  @Test
  fun `domain event does not trigger JDA processing when no PENDING investigations`() {
    val prisonerNumber = "A1234BC"
    val investigationId = UUID.randomUUID()
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonerNumber))
      .thenReturn(
        PersonCaseNoteInvestigationResolution(
          prisonerNumber,
          listOf(investigationId),
          listOf(InvestigationEvaluation(investigationId, "INVESTIGATION_COMPLETE", false, "status is not INVESTIGATION_PENDING")),
        ),
      )

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonerNumber))

    verify(jdaService, never()).submitCaseNotesForAnalysis(any(), any(), any())
    verify(jdaService, never()).submitCaseNotesForReAnalysis(any(), any(), any())
  }

  @Test
  fun `domain event triggers JDA re-analysis when pending investigations exist and they have already been analysed by JDA before`(output: CapturedOutput) {
    val prisonerNumber = "A1234BC"
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val investigationId = UUID.randomUUID()
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonerNumber))
      .thenReturn(
        PersonCaseNoteInvestigationResolution(
          prisonerNumber,
          listOf(investigationId),
          listOf(InvestigationEvaluation(investigationId, "INVESTIGATION_PENDING", true)),
        ),
      )
    whenever(caseNoteAnalysedRepository.existsByInvestigationId(investigationId)).thenReturn(true)

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonerNumber))

    verify(jdaService).submitCaseNotesForReAnalysis(prisonerNumber, investigationId, caseNoteId)
    verify(jdaService, never()).submitCaseNotesForAnalysis(any(), any(), any())
    assertThat(output.out).contains("Triggering JDA re-analysis for investigationId=")
  }

  @Test
  fun `domain event triggers complete initial JDA analysis when pending investigations exist but none have been analysed by JDA before`(output: CapturedOutput) {
    val prisonerNumber = "A1234BC"
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val investigationId = UUID.randomUUID()
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonerNumber))
      .thenReturn(
        PersonCaseNoteInvestigationResolution(
          prisonerNumber,
          listOf(investigationId),
          listOf(InvestigationEvaluation(investigationId, "INVESTIGATION_PENDING", true)),
        ),
      )
    whenever(caseNoteAnalysedRepository.existsByInvestigationId(investigationId)).thenReturn(false)
    whenever(caseNotesService.getCaseNote(prisonerNumber, caseNoteId)).thenReturn(testCaseNote())

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonerNumber))
    verify(jdaService, never()).submitCaseNotesForReAnalysis(prisonerNumber, investigationId, caseNoteId)
    verify(jdaService).submitCaseNotesForAnalysis(prisonerNumber, "MDI", investigationId.toString())
    assertThat(output.out).contains("does not exist in case_notes_analysed; triggering initial JDA analysis")
  }

  @Test
  fun `missing personReference logs warning and exits`(output: CapturedOutput) {
    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, personReference = null))

    verify(personCaseNoteInvestigationService, never()).resolveInvestigations(any())
    assertThat(output.out).contains("because no NOMS prisoner number was provided in personReference")
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

    verify(personCaseNoteInvestigationService, never()).resolveInvestigations(any())
    assertThat(output.out).contains("because no NOMS prisoner number was provided in personReference")
  }

  private fun testCaseNote() = CaseNote(
    caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
    offenderIdentifier = "A1234BC",
    type = "OBSERVE",
    typeDescription = "Observation",
    subType = "GEN",
    subTypeDescription = "General",
    creationDateTime = LocalDateTime.now(),
    occurrenceDateTime = LocalDateTime.now(),
    authorName = "Test User",
    authorUserId = "user-1",
    authorUsername = "user1",
    text = "Case note text",
    locationId = "MDI",
    sensitive = false,
    amendments = emptyList(),
  )

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

  private fun testJdaRequestResponse(investigationId: UUID, caseNoteId: UUID) = JdaRequestResponse(
    requestId = UUID.randomUUID(),
    correlationId = investigationId,
    prompt = JdaPrompt(
      key = "case-note-analysis",
      version = 1,
    ),
    status = JdaRequestStatus.SUCCEEDED,
    responseData = listOf(
      JdaDequeueResponseData(
        caseNoteId = caseNoteId,
        justifyingSpans = listOf(
          JustifyingSpan(
            text = "annotated text",
            justifies = BehaviourType.RISKS_AND_TRIGGERS,
          ),
        ),
      ),
    ),
    metaData = JdaMetadata(
      requestType = JdaRequestType.SYNC,
      submittedAt = OffsetDateTime.now(),
    ),
  )
}
