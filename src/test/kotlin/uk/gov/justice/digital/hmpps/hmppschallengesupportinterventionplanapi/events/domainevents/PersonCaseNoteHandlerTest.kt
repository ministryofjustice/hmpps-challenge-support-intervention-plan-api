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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JustifyingSpan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNoteAnnotationsService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.JdaService
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

@ExtendWith(OutputCaptureExtension::class)
class PersonCaseNoteHandlerTest {
  private val personCaseNoteInvestigationService = mock<PersonCaseNoteInvestigationService>()
  private val jdaService = mock<JdaService>()
  private val caseNoteAnnotationsService = mock<CaseNoteAnnotationsService>()
  private val handler = PersonCaseNoteHandler(personCaseNoteInvestigationService, jdaService, caseNoteAnnotationsService)

  @Test
  fun `created event with valid prison number resolves investigations and logs receipt`(output: CapturedOutput) {
    val prisonNumber = "A1234BC"
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonNumber))
      .thenReturn(PersonCaseNoteInvestigationResolution(prisonNumber, emptyList(), emptyList()))

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonNumber))

    verify(personCaseNoteInvestigationService, times(1)).resolveInvestigations(prisonNumber)
    verify(jdaService, never()).submitCaseNotesForReAnalysis(any(), any(), any())
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
    verify(jdaService, never()).submitCaseNotesForReAnalysis(any(), any(), any())
    assertThat(output.out).contains("Received person.case-note.updated event")
    assertThat(output.out).contains("Resolved prison number A1234BC")
  }

  @Test
  fun `eligible investigations trigger re-analysis submissions`() {
    val prisonNumber = "A1234BC"
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val investigationId1 = UUID.randomUUID()
    val investigationId2 = UUID.randomUUID()
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonNumber))
      .thenReturn(
        PersonCaseNoteInvestigationResolution(
          prisonNumber,
          listOf(investigationId1, investigationId2),
          listOf(
            InvestigationEvaluation(investigationId1, "INVESTIGATION_PENDING", true),
            InvestigationEvaluation(investigationId2, "INVESTIGATION_PENDING", true),
          ),
        ),
      )

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonNumber))

    verify(jdaService, times(1)).submitCaseNotesForReAnalysis(prisonNumber, investigationId1, caseNoteId)
    verify(jdaService, times(1)).submitCaseNotesForReAnalysis(prisonNumber, investigationId2, caseNoteId)
  }

  @Test
  fun `eligible investigations persist synchronous annotations when re-analysis returns a response`() {
    val prisonNumber = "A1234BC"
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val investigationId = UUID.randomUUID()
    val response = testJdaRequestResponse(investigationId, caseNoteId)
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonNumber))
      .thenReturn(
        PersonCaseNoteInvestigationResolution(
          prisonNumber,
          listOf(investigationId),
          listOf(InvestigationEvaluation(investigationId, "INVESTIGATION_PENDING", true)),
        ),
      )
    whenever(jdaService.submitCaseNotesForReAnalysis(prisonNumber, investigationId, caseNoteId)).thenReturn(response)

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonNumber))

    verify(jdaService).submitCaseNotesForReAnalysis(prisonNumber, investigationId, caseNoteId)
    verify(caseNoteAnnotationsService).persistSynchronousAnnotations(response, prisonNumber)
  }

  @Test
  fun `eligible investigations do not persist annotations when re-analysis returns null`() {
    val prisonNumber = "A1234BC"
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val investigationId = UUID.randomUUID()
    whenever(personCaseNoteInvestigationService.resolveInvestigations(prisonNumber))
      .thenReturn(
        PersonCaseNoteInvestigationResolution(
          prisonNumber,
          listOf(investigationId),
          listOf(InvestigationEvaluation(investigationId, "INVESTIGATION_PENDING", true)),
        ),
      )
    whenever(jdaService.submitCaseNotesForReAnalysis(prisonNumber, investigationId, caseNoteId)).thenReturn(null)

    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, prisonNumber = prisonNumber))

    verify(jdaService).submitCaseNotesForReAnalysis(prisonNumber, investigationId, caseNoteId)
    verify(caseNoteAnnotationsService, never()).persistSynchronousAnnotations(any(), any())
  }

  @Test
  fun `missing personReference logs warning and exits`(output: CapturedOutput) {
    handler.handle(caseNoteEvent(DomainEventsListener.PERSON_CASE_NOTE_CREATED, personReference = null))

    verify(personCaseNoteInvestigationService, never()).resolveInvestigations(any())
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

    verify(personCaseNoteInvestigationService, never()).resolveInvestigations(any())
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
