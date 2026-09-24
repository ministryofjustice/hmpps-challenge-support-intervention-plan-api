package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNoteAmendment
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.JdaDequeueResponseStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JustifyingSpan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

class CaseNoteAnnotationsServiceTest {
  private val caseNotesClient = mock<CaseNotesClient>()
  private val caseNoteAnalysedRepository = mock<CaseNoteAnalysedRepository>()
  private val caseNoteAnnotationRepository = mock<CaseNoteAnnotationRepository>()
  private val jdbcTemplate = mock<NamedParameterJdbcTemplate>()
  private val csipRecordService = mock<CsipRecordService>()
  private val caseNotesService = CaseNotesService(caseNotesClient)
  private val jdaService = mock<JdaService>()
  private val personSummaryService = mock<PersonSummaryService>()
  private val service = CaseNoteAnnotationsService(
    caseNotesService,
    jdaService,
    caseNoteAnalysedRepository,
    caseNoteAnnotationRepository,
    jdbcTemplate,
    personSummaryService,
    csipRecordService,
    Duration.ofSeconds(30),
  )
  private val referralId = UUID.fromString("9ec1ca0c-0d92-4ae4-b307-0a57759ac52e")

  @BeforeEach
  fun setUp() {
    whenever(caseNoteAnalysedRepository.save(any<CaseNoteAnalysed>())).thenAnswer { it.getArgument(0) }
    whenever(jdbcTemplate.update(any<String>(), any<MapSqlParameterSource>())).thenReturn(1)
  }

  @Test
  fun `processQueuedCaseNoteAnnotations handles an empty queue gracefully`() {
    whenever(jdaService.getCaseNoteAnnotationsFromQueue()).thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    verify(jdaService, times(1)).getCaseNoteAnnotationsFromQueue()
    verify(caseNoteAnalysedRepository, never()).save(any())
    verify(caseNoteAnnotationRepository, never()).save(any())
    verify(csipRecordService, never()).retrieveCsipRecord(any())
  }

  @Test
  fun `processQueuedCaseNoteAnnotations persists analysed rows and annotations`() {
    stubCsipRecordLookup()

    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(testResponse())
      .thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    val analysedCaptor = argumentCaptor<CaseNoteAnalysed>()
    verify(caseNoteAnalysedRepository, times(1)).save(analysedCaptor.capture())
    assertThat(analysedCaptor.firstValue.prisonerNumber).isEqualTo("A1234BC")
    assertThat(analysedCaptor.firstValue.promptKey).isEqualTo("case-note-analysis")
    assertThat(analysedCaptor.firstValue.promptVersion).isEqualTo(3)
    assertThat(analysedCaptor.firstValue.usualBehaviourRelevancy).isEqualTo(3)
    assertThat(analysedCaptor.firstValue.risksAndTriggersRelevancy).isEqualTo(2)
    assertThat(analysedCaptor.firstValue.protectiveFactorsRelevancy).isEqualTo(4)

    verify(jdbcTemplate, times(4)).update(any<String>(), any<MapSqlParameterSource>())
  }

  @Test
  fun `processQueuedCaseNoteAnnotations continues when csip lookup fails`() {
    val response = testResponse()

    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response)
      .thenReturn(null)
    whenever(csipRecordService.retrieveCsipRecord(any()))
      .thenThrow(RuntimeException("Something went wrong"))

    service.processQueuedCaseNoteAnnotations()

    verify(jdaService, times(2)).getCaseNoteAnnotationsFromQueue()
    verify(csipRecordService, times(1)).retrieveCsipRecord(any())
    verify(caseNoteAnalysedRepository, never()).save(any())
    verify(jdbcTemplate, never()).update(any<String>(), any<MapSqlParameterSource>())
  }

  @Test
  fun `persistSynchronousAnnotations persists analysed rows and annotations`() {
    val requestId = UUID.randomUUID()
    val prisonerNumber = "A1234BC"
    val response = testJdaRequestResponse(requestId)

    service.persistSynchronousAnnotations(response, prisonerNumber)

    val analysedCaptor = argumentCaptor<CaseNoteAnalysed>()
    verify(caseNoteAnalysedRepository, times(1)).save(analysedCaptor.capture())
    assertThat(analysedCaptor.firstValue.prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(analysedCaptor.firstValue.protectiveFactorsRelevancy).isEqualTo(4)

    verify(jdbcTemplate, times(4)).update(any<String>(), any<MapSqlParameterSource>())
  }

  @Test
  fun `buildSuggestedCaseNotes returns relevance derived from annotations`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    whenever(caseNoteAnalysedRepository.findByPrisonerNumberAndInvestigationId("A1234AA", referralId))
      .thenReturn(
        listOf(
          CaseNoteAnalysed(
            requestId = UUID.randomUUID(),
            investigationId = referralId,
            prisonerNumber = "A1234AA",
            caseNoteId = caseNoteId,
            promptKey = "case-note-analysis",
            promptVersion = 3,
            usualBehaviourRelevancy = 0,
            risksAndTriggersRelevancy = 3,
            protectiveFactorsRelevancy = 0,
          ),
        ),
      )
    whenever(caseNoteAnnotationRepository.findByCaseNotesAnalysedIdInAndBehaviourType(any(), eq(BehaviourType.RISKS_AND_TRIGGERS)))
      .thenReturn(
        listOf(
          annotation(caseNoteId = caseNoteId, annotatedText = "became agitated", confidenceLevel = ConfidenceLevel.LOW),
          annotation(caseNoteId = caseNoteId, annotatedText = "raised his voice", confidenceLevel = ConfidenceLevel.HIGH),
        ),
      )
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId))
      .thenReturn(caseNote(caseNoteId, text = "Prisoner became agitated and raised his voice."))

    val response = service.buildSuggestedCaseNotes("A1234AA", referralId, suggestedRequest())

    assertThat(response.suggestedCaseNotes).hasSize(1)
    assertThat(response.suggestedCaseNotes.first().relevance).isEqualTo("high")
    assertThat(response.suggestedCaseNotes.first().annotatedCaseNote)
      .contains("<span class=\"annotation-type\">became agitated</span>")
      .contains("<span class=\"annotation-type\">raised his voice</span>")
  }

  @Test
  fun `getCaseNotesWithAnnotations groups annotations under one case note`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    whenever(caseNoteAnalysedRepository.findByPrisonerNumberAndInvestigationId("A1234AA", referralId))
      .thenReturn(
        listOf(
          CaseNoteAnalysed(
            requestId = UUID.randomUUID(),
            investigationId = referralId,
            prisonerNumber = "A1234AA",
            caseNoteId = caseNoteId,
            promptKey = "case-note-analysis",
            promptVersion = 3,
            usualBehaviourRelevancy = 0,
            risksAndTriggersRelevancy = 3,
            protectiveFactorsRelevancy = 0,
          ),
        ),
      )
    whenever(caseNoteAnnotationRepository.findByCaseNotesAnalysedIdInAndBehaviourType(any(), eq(BehaviourType.RISKS_AND_TRIGGERS)))
      .thenReturn(
        listOf(
          annotation(caseNoteId = caseNoteId, annotatedText = "text 1"),
          annotation(caseNoteId = caseNoteId, annotatedText = "text 2"),
        ),
      )
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId)).thenReturn(caseNote(caseNoteId))

    val result = service.getCaseNotesWithAnnotations("A1234AA", BehaviourType.RISKS_AND_TRIGGERS, referralId)

    assertThat(result).hasSize(1)
    assertThat(result.first().annotations.mapNotNull { it.annotatedText }).containsExactlyInAnyOrder("text 1", "text 2")
  }

  @Test
  fun `buildSuggestedCaseNotes throws IllegalArgumentException when prisoner does not exist`() {
    doThrow(IllegalArgumentException("Prisoner number invalid")).whenever(personSummaryService)
      .validatePrisoner("NOT_FOUND")

    val exception = assertThrows<IllegalArgumentException> {
      service.buildSuggestedCaseNotes("NOT_FOUND", referralId, suggestedRequest())
    }

    assertThat(exception.message).isEqualTo("Prisoner number invalid")
    verify(personSummaryService).validatePrisoner("NOT_FOUND")
    verify(caseNoteAnnotationRepository, never()).findByPrisonerNumberAndBehaviourType(any(), any())
  }

  private fun stubCsipRecordLookup(prisonNumber: String = "A1234BC") {
    val csipRecord = mock<CsipRecord>()
    whenever(csipRecord.prisonNumber).thenReturn(prisonNumber)
    whenever(csipRecordService.retrieveCsipRecord(any())).thenReturn(csipRecord)
  }

  private fun annotation(
    caseNoteId: UUID,
    annotatedText: String,
    behaviourType: BehaviourType = BehaviourType.RISKS_AND_TRIGGERS,
    confidenceLevel: ConfidenceLevel = ConfidenceLevel.HIGH,
  ): CaseNoteAnnotation {
    val relevancy = when (confidenceLevel) {
      ConfidenceLevel.LOW -> 1
      ConfidenceLevel.MEDIUM -> 2
      ConfidenceLevel.HIGH -> 3
    }

    val analysed = CaseNoteAnalysed(
      requestId = UUID.randomUUID(),
      investigationId = UUID.randomUUID(),
      prisonerNumber = "A1234AA",
      caseNoteId = caseNoteId,
      promptKey = "case-note-analysis",
      promptVersion = 3,
      usualBehaviourRelevancy = if (behaviourType == BehaviourType.USUAL_BEHAVIOUR_PRESENTATION) relevancy else 0,
      risksAndTriggersRelevancy = if (behaviourType == BehaviourType.RISKS_AND_TRIGGERS) relevancy else 0,
      protectiveFactorsRelevancy = if (behaviourType == BehaviourType.PROTECTIVE_FACTORS) relevancy else 0,
    )

    return CaseNoteAnnotation(
      requestId = UUID.randomUUID(),
      investigationId = analysed.investigationId,
      caseNotesAnalysed = analysed,
      caseNoteId = caseNoteId,
      behaviourType = behaviourType,
      annotatedText = annotatedText,
      createdDate = LocalDateTime.now(),
    )
  }

  private fun caseNote(
    caseNoteId: UUID,
    text: String = "Case note text",
    creationDateTime: LocalDateTime = LocalDateTime.now(),
    amendments: List<CaseNoteAmendment> = emptyList(),
  ) = CaseNote(
    caseNoteId = caseNoteId,
    offenderIdentifier = "A1234AA",
    type = "GEN",
    typeDescription = "General",
    subType = "OBS",
    subTypeDescription = "Observation",
    creationDateTime = creationDateTime,
    occurrenceDateTime = creationDateTime,
    authorName = "Test User",
    authorUserId = "USER1",
    authorUsername = "testuser",
    text = text,
    locationId = "MDI",
    sensitive = false,
    amendments = amendments,
  )

  private fun suggestedRequest() = SuggestedCaseNotesRequest(
    referralId = referralId,
    behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
    sortField = "relevance",
    sortOrder = "desc",
  )

  private fun testResponse() = JdaDequeueResponse(
    requestId = UUID.fromString("f091bc73-4f88-4ff6-9e50-5148d29ed3f6"),
    correlationId = UUID.fromString("f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb"),
    prompt = JdaPrompt(
      key = "case-note-analysis",
      version = 3,
    ),
    status = JdaDequeueResponseStatus.SUCCEEDED,
    responseData = listOf(
      JdaDequeueResponseData(
        caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        usualBehaviourPresentation = 3,
        risksAndTriggers = 2,
        protectiveFactors = 4,
        comment = "test comment",
        justifyingSpans = listOf(
          JustifyingSpan(text = "annotated text 1", justifies = BehaviourType.PROTECTIVE_FACTORS),
          JustifyingSpan(text = "annotated text 2", justifies = BehaviourType.RISKS_AND_TRIGGERS),
          JustifyingSpan(text = "annotated text 3", justifies = BehaviourType.USUAL_BEHAVIOUR_PRESENTATION),
          JustifyingSpan(text = "annotated text 4", justifies = BehaviourType.PROTECTIVE_FACTORS),
        ),
      ),
    ),
    metaData = JdaDequeueResponseMetadata(
      requestType = JdaRequestType.ASYNC,
      completedAt = LocalDateTime.now(),
      completionMs = 1200,
    ),
  )

  private fun testJdaRequestResponse(requestId: UUID = UUID.randomUUID()) = JdaRequestResponse(
    requestId = requestId,
    correlationId = UUID.randomUUID(),
    prompt = JdaPrompt(
      key = "case-note-analysis",
      version = 3,
    ),
    status = JdaRequestStatus.SUCCEEDED,
    responseData = listOf(
      JdaDequeueResponseData(
        caseNoteId = UUID.randomUUID(),
        usualBehaviourPresentation = 3,
        risksAndTriggers = 2,
        protectiveFactors = 4,
        justifyingSpans = listOf(
          JustifyingSpan(text = "annotated text 1", justifies = BehaviourType.PROTECTIVE_FACTORS),
          JustifyingSpan(text = "annotated text 2", justifies = BehaviourType.RISKS_AND_TRIGGERS),
          JustifyingSpan(text = "annotated text 3", justifies = BehaviourType.USUAL_BEHAVIOUR_PRESENTATION),
          JustifyingSpan(text = "annotated text 4", justifies = BehaviourType.PROTECTIVE_FACTORS),
        ),
      ),
    ),
    metaData = JdaMetadata(
      requestType = JdaRequestType.SYNC,
      submittedAt = java.time.OffsetDateTime.now(),
    ),
  )
}
