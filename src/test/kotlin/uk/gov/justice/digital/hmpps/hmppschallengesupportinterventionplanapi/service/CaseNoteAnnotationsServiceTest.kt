package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNoteAmendment
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda.JdaClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.JdaDequeueResponseStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteAnnotationSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteWithAnnotations
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
  private val jdaClient = mock<JdaClient>()
  private val caseNoteAnnotationRepository = mock<CaseNoteAnnotationRepository>()
  private val csipRecordService = mock<CsipRecordService>()
  private val service = CaseNoteAnnotationsService(
    caseNotesClient,
    jdaClient,
    caseNoteAnnotationRepository,
    csipRecordService,
    Duration.ofSeconds(30),
  )

  @Test
  fun `processQueuedCaseNoteAnnotations handles an empty queue gracefully`() {
    whenever(jdaClient.getCaseNoteAnnotationsFromQueue()).thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    verify(jdaClient, times(1)).getCaseNoteAnnotationsFromQueue()
    verify(caseNoteAnnotationRepository, never()).save(any())
    verify(csipRecordService, never()).retrieveCsipRecord(any())
  }

  @Test
  fun `getCaseNoteAnnotationsFromQueue propagates downstream failures`() {
    whenever(jdaClient.getCaseNoteAnnotationsFromQueue())
      .thenThrow(
        DownstreamServiceException(
          "Get case note annotations from queue failed",
          RuntimeException("Something went wrong"),
        ),
      )

    assertThrows<DownstreamServiceException> {
      service.processQueuedCaseNoteAnnotations()
    }

    verify(caseNoteAnnotationRepository, never()).save(any())
  }

  @Test
  fun `processQueuedCaseNoteAnnotations continues when csip lookup fails`() {
    val response = testResponse()

    whenever(jdaClient.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response)
      .thenReturn(null)
    whenever(csipRecordService.retrieveCsipRecord(any()))
      .thenThrow(RuntimeException("Something went wrong"))

    service.processQueuedCaseNoteAnnotations()

    verify(jdaClient, times(2)).getCaseNoteAnnotationsFromQueue()
    verify(csipRecordService, times(1)).retrieveCsipRecord(any())
    verify(caseNoteAnnotationRepository, never()).save(any())
  }

  @Test
  fun `processQueuedCaseNoteAnnotations skips responses with no response data`() {
    val responseWithoutData = JdaDequeueResponse(
      requestId = UUID.randomUUID(),
      correlationId = UUID.randomUUID(),
      prompt = JdaPrompt(
        key = "case-note-analysis",
        version = 3,
      ),
      status = JdaDequeueResponseStatus.SUCCEEDED,
      responseData = null,
      metaData = JdaDequeueResponseMetadata(
        requestType = JdaRequestType.ASYNC,
        completedAt = LocalDateTime.now(),
        completionMs = 1200,
      ),
    )

    whenever(jdaClient.getCaseNoteAnnotationsFromQueue())
      .thenReturn(responseWithoutData)
      .thenReturn(null)
    stubCsipRecordLookup()

    service.processQueuedCaseNoteAnnotations()

    verify(jdaClient, times(2)).getCaseNoteAnnotationsFromQueue()
    verify(csipRecordService, times(1)).retrieveCsipRecord(any())
    verify(caseNoteAnnotationRepository, never()).save(any())
  }

  @Test
  fun `getCaseNoteAnnotationsFromQueue polls until dequeue returns null`() {
    stubCsipRecordLookup()

    whenever(jdaClient.getCaseNoteAnnotationsFromQueue())
      .thenReturn(testResponse())
      .thenReturn(testResponse())
      .thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    verify(jdaClient, times(3)).getCaseNoteAnnotationsFromQueue()
    val annotationCaptor = argumentCaptor<CaseNoteAnnotation>()
    verify(caseNoteAnnotationRepository, times(8)).save(annotationCaptor.capture())
    assert(annotationCaptor.allValues.all { it.prisonerNumber == "A1234BC" })
    assert(annotationCaptor.allValues.all { it.annotatedText?.startsWith("annotated text") == true })
    assert(
      annotationCaptor.allValues.mapNotNull { it.behaviourType }.toSet() ==
        setOf(
          BehaviourType.PROTECTIVE_FACTORS,
          BehaviourType.RISKS_AND_TRIGGERS,
          BehaviourType.USUAL_BEHAVIOUR_PRESENTATION,
        ),
    )
  }

  @Test
  fun `getCaseNoteAnnotationsFromQueue continues draining when persisting an item fails`() {
    stubCsipRecordLookup()

    val firstResponse = testResponse()
    val secondResponse = testResponse()
    val thirdResponse = testResponse()

    whenever(jdaClient.getCaseNoteAnnotationsFromQueue())
      .thenReturn(firstResponse)
      .thenReturn(secondResponse)
      .thenReturn(thirdResponse)
      .thenReturn(null)

    var persistAttempts = 0
    whenever(
      caseNoteAnnotationRepository.save(
        any<CaseNoteAnnotation>(),
      ),
    ).thenAnswer {
      persistAttempts++
      if (persistAttempts == 2) {
        throw RuntimeException("Something went wrong")
      }
      it.getArgument<CaseNoteAnnotation>(0)
    }

    service.processQueuedCaseNoteAnnotations()

    verify(jdaClient, times(4)).getCaseNoteAnnotationsFromQueue()
    val annotationCaptor = argumentCaptor<CaseNoteAnnotation>()
    verify(caseNoteAnnotationRepository, times(12)).save(annotationCaptor.capture())
    assert(annotationCaptor.allValues.size == 12)
  }

  @Test
  fun `persistSynchronousAnnotations persists annotations from synchronous response`() {
    val requestId = UUID.randomUUID()
    val prisonerNumber = "A1234BC"
    val response = testJdaRequestResponse(requestId)

    service.persistSynchronousAnnotations(response, prisonerNumber)

    val annotationCaptor = argumentCaptor<CaseNoteAnnotation>()
    verify(caseNoteAnnotationRepository, times(4)).save(annotationCaptor.capture())

    val savedAnnotations = annotationCaptor.allValues
    assert(savedAnnotations.all { it.prisonerNumber == prisonerNumber })
    assert(savedAnnotations.all { it.requestId == requestId })
    assert(savedAnnotations.all { it.promptKey == "case-note-analysis" })
    assert(savedAnnotations.all { it.promptVersion == 0 })
    assert(savedAnnotations.mapNotNull { it.behaviourType }.toSet().size == 3)
  }

  @Test
  fun `persistSynchronousAnnotations handles response with no data gracefully`() {
    val requestId = UUID.randomUUID()
    val prisonerNumber = "A1234BC"
    val response = testJdaRequestResponse(requestId).copy(responseData = null)

    service.persistSynchronousAnnotations(response, prisonerNumber)

    verify(caseNoteAnnotationRepository, never()).save(any())
  }

  @Test
  fun `persistSynchronousAnnotations handles response with empty data gracefully`() {
    val requestId = UUID.randomUUID()
    val prisonerNumber = "A1234BC"
    val response = testJdaRequestResponse(requestId).copy(responseData = emptyList())

    service.persistSynchronousAnnotations(response, prisonerNumber)

    verify(caseNoteAnnotationRepository, never()).save(any())
  }

  @Test
  fun `persistSynchronousAnnotations continues when saving an annotation fails`() {
    val requestId = UUID.randomUUID()
    val prisonerNumber = "A1234BC"
    val response = testJdaRequestResponse(requestId)

    var saveAttempts = 0
    whenever(caseNoteAnnotationRepository.save(any<CaseNoteAnnotation>()))
      .thenAnswer {
        saveAttempts++
        if (saveAttempts == 3) {
          throw RuntimeException("Database error")
        }
        it.getArgument<CaseNoteAnnotation>(0)
      }

    service.persistSynchronousAnnotations(response, prisonerNumber)

    val annotationCaptor = argumentCaptor<CaseNoteAnnotation>()
    verify(caseNoteAnnotationRepository, times(4)).save(annotationCaptor.capture())
  }

  @Test
  fun `persistAnnotationsFromDequeue continues when saving an annotation fails`() {
    stubCsipRecordLookup()

    val response = testResponse()
    whenever(jdaClient.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response)
      .thenReturn(null)

    var saveAttempts = 0
    whenever(caseNoteAnnotationRepository.save(any<CaseNoteAnnotation>()))
      .thenAnswer {
        saveAttempts++
        if (saveAttempts == 3) {
          throw RuntimeException("Database error")
        }
        it.getArgument<CaseNoteAnnotation>(0)
      }

    service.processQueuedCaseNoteAnnotations()

    val annotationCaptor = argumentCaptor<CaseNoteAnnotation>()
    verify(caseNoteAnnotationRepository, times(4)).save(annotationCaptor.capture())
  }

  @Test
  fun `processQueuedCaseNoteAnnotations stops fetching new messages after configured max processing duration`() {
    stubCsipRecordLookup()

    val timeoutService = CaseNoteAnnotationsService(
      caseNotesClient,
      jdaClient,
      caseNoteAnnotationRepository,
      csipRecordService,
      Duration.ofMillis(50),
    )

    whenever(jdaClient.getCaseNoteAnnotationsFromQueue())
      .thenReturn(testResponse())
      .thenReturn(testResponse())

    var saveAttempts = 0
    whenever(caseNoteAnnotationRepository.save(any<CaseNoteAnnotation>()))
      .thenAnswer {
        saveAttempts++
        if (saveAttempts == 1) {
          Thread.sleep(100)
        }
        it.getArgument<CaseNoteAnnotation>(0)
      }

    timeoutService.processQueuedCaseNoteAnnotations()

    verify(jdaClient, times(1)).getCaseNoteAnnotationsFromQueue()
    verify(caseNoteAnnotationRepository, times(4)).save(any())
  }

  @Test
  fun `buildSuggestedCaseNotes returns response header fields from request`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(listOf(annotation(caseNoteId = caseNoteId, annotatedText = "became agitated")))
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId)).thenReturn(caseNote(caseNoteId))

    val request = suggestedRequest()
    val response = service.buildSuggestedCaseNotes("A1234AA", request)

    assertThat(response.prisonerNumber).isEqualTo("A1234AA")
    assertThat(response.behaviourType).isEqualTo(BehaviourType.RISKS_AND_TRIGGERS)
    assertThat(response.sortField).isEqualTo("createdDate")
    assertThat(response.sortOrder).isEqualTo("desc")
  }

  @Test
  fun `buildSuggestedCaseNotes returns single suggested case note for one case note with one annotation`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    val createdAt = LocalDateTime.of(2025, 6, 1, 9, 0)
    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(listOf(annotation(caseNoteId = caseNoteId, annotatedText = "became agitated", confidenceLevel = ConfidenceLevel.HIGH)))
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId))
      .thenReturn(caseNote(caseNoteId, text = "Prisoner became agitated during the session.", creationDateTime = createdAt))

    val response = service.buildSuggestedCaseNotes("A1234AA", suggestedRequest())

    assertThat(response.suggestedCaseNotes).hasSize(1)
    val note = response.suggestedCaseNotes.first()
    assertThat(note.caseNoteId).isEqualTo(caseNoteId)
    assertThat(note.createdAt).isEqualTo(createdAt)
    assertThat(note.relevance).isEqualTo("high")
    assertThat(note.annotatedCaseNote).contains("<span class=\"annotation-type\">became agitated</span>")
  }

  @Test
  fun `buildSuggestedCaseNotes returns one suggested case note per case note`() {
    val caseNoteIdOne = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    val caseNoteIdTwo = UUID.fromString("223e4567-e89b-12d3-a456-426614174000")
    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(
        listOf(
          annotation(caseNoteId = caseNoteIdOne, annotatedText = "agitated"),
          annotation(caseNoteId = caseNoteIdTwo, annotatedText = "raised his voice"),
        ),
      )
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteIdOne))
      .thenReturn(caseNote(caseNoteIdOne, text = "Prisoner was agitated."))
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteIdTwo))
      .thenReturn(caseNote(caseNoteIdTwo, text = "Prisoner raised his voice."))

    val response = service.buildSuggestedCaseNotes("A1234AA", suggestedRequest())

    assertThat(response.suggestedCaseNotes).hasSize(2)
    assertThat(response.suggestedCaseNotes.map { it.caseNoteId }).containsExactlyInAnyOrder(caseNoteIdOne, caseNoteIdTwo)
  }

  @Test
  fun `buildSuggestedCaseNotes uses highest confidence level across annotations for a case note`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(
        listOf(
          annotation(caseNoteId = caseNoteId, annotatedText = "became agitated", confidenceLevel = ConfidenceLevel.LOW),
          annotation(caseNoteId = caseNoteId, annotatedText = "raised his voice", confidenceLevel = ConfidenceLevel.HIGH),
        ),
      )
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId))
      .thenReturn(caseNote(caseNoteId, text = "Prisoner became agitated and raised his voice."))

    val response = service.buildSuggestedCaseNotes("A1234AA", suggestedRequest())

    assertThat(response.suggestedCaseNotes).hasSize(1)
    assertThat(response.suggestedCaseNotes.first().relevance).isEqualTo("high")
  }

  @Test
  fun `buildSuggestedCaseNotes orders suggested case notes by creationDateTime descending`() {
    val olderCaseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    val newerCaseNoteId = UUID.fromString("223e4567-e89b-12d3-a456-426614174000")
    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(
        listOf(
          annotation(caseNoteId = olderCaseNoteId, annotatedText = "agitated"),
          annotation(caseNoteId = newerCaseNoteId, annotatedText = "raised his voice"),
        ),
      )
    val older = LocalDateTime.of(2025, 1, 1, 9, 0)
    val newer = LocalDateTime.of(2025, 6, 1, 9, 0)
    whenever(caseNotesClient.getCaseNote("A1234AA", olderCaseNoteId))
      .thenReturn(caseNote(olderCaseNoteId, text = "Prisoner was agitated.", creationDateTime = older))
    whenever(caseNotesClient.getCaseNote("A1234AA", newerCaseNoteId))
      .thenReturn(caseNote(newerCaseNoteId, text = "Prisoner raised his voice.", creationDateTime = newer))

    val response = service.buildSuggestedCaseNotes("A1234AA", suggestedRequest())

    assertThat(response.suggestedCaseNotes).hasSize(2)
    assertThat(response.suggestedCaseNotes[0].caseNoteId).isEqualTo(newerCaseNoteId)
    assertThat(response.suggestedCaseNotes[1].caseNoteId).isEqualTo(olderCaseNoteId)
  }

  @Test
  fun `buildSuggestedCaseNotes applies default sortField and sortOrder when omitted from request`() {
    val setup = setupThreeCaseNotesForSorting()

    val request = SuggestedCaseNotesRequest(
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
    )

    val response = service.buildSuggestedCaseNotes("A1234AA", request)

    assertThat(response.sortField).isEqualTo("createdDate")
    assertThat(response.sortOrder).isEqualTo("desc")
    assertThat(response.suggestedCaseNotes).hasSize(3)
    assertThat(response.suggestedCaseNotes[0].caseNoteId).isEqualTo(setup.newerCaseNoteId)
    assertThat(response.suggestedCaseNotes[1].caseNoteId).isEqualTo(setup.middleCaseNoteId)
    assertThat(response.suggestedCaseNotes[2].caseNoteId).isEqualTo(setup.olderCaseNoteId)
  }

  @Test
  fun `buildSuggestedCaseNotes orders suggested case notes by creationDateTime ascending`() {
    val setup = setupThreeCaseNotesForSorting()

    val request = SuggestedCaseNotesRequest(
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      sortField = "createdDate",
      sortOrder = "asc",
    )

    val response = service.buildSuggestedCaseNotes("A1234AA", request)

    assertThat(response.sortOrder).isEqualTo("asc")
    assertThat(response.suggestedCaseNotes).hasSize(3)
    assertThat(response.suggestedCaseNotes[0].caseNoteId).isEqualTo(setup.olderCaseNoteId)
    assertThat(response.suggestedCaseNotes[1].caseNoteId).isEqualTo(setup.middleCaseNoteId)
    assertThat(response.suggestedCaseNotes[2].caseNoteId).isEqualTo(setup.newerCaseNoteId)
  }

  @Test
  fun `buildSuggestedCaseNotes orders suggested case notes by creationDateTime descending when sortOrder is desc`() {
    val setup = setupThreeCaseNotesForSorting()

    val ascRequest = SuggestedCaseNotesRequest(
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      sortField = "createdDate",
      sortOrder = "desc",
    )

    val response = service.buildSuggestedCaseNotes("A1234AA", ascRequest)

    assertThat(response.sortOrder).isEqualTo("desc")
    assertThat(response.suggestedCaseNotes).hasSize(3)
    assertThat(response.suggestedCaseNotes[0].caseNoteId).isEqualTo(setup.newerCaseNoteId)
    assertThat(response.suggestedCaseNotes[1].caseNoteId).isEqualTo(setup.middleCaseNoteId)
    assertThat(response.suggestedCaseNotes[2].caseNoteId).isEqualTo(setup.olderCaseNoteId)
  }

  @Test
  fun `buildSuggestedCaseNotes orders by lastAmendedDate descending when amendment is newer than creation date`() {
    val olderCaseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    val middleCaseNoteId = UUID.fromString("173e4567-e89b-12d3-a456-426614174000")
    val newerCaseNoteId = UUID.fromString("223e4567-e89b-12d3-a456-426614174000")

    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(
        listOf(
          annotation(caseNoteId = olderCaseNoteId, annotatedText = "older"),
          annotation(caseNoteId = middleCaseNoteId, annotatedText = "middle"),
          annotation(caseNoteId = newerCaseNoteId, annotatedText = "newer"),
        ),
      )

    val older = LocalDateTime.of(2025, 1, 1, 9, 0)
    val middle = LocalDateTime.of(2025, 3, 1, 9, 0)
    val newer = LocalDateTime.of(2025, 6, 1, 9, 0)
    val oldestAmendment = LocalDateTime.of(2024, 12, 1, 9, 0)
    val amendedLatest = LocalDateTime.of(2025, 7, 1, 9, 0)

    whenever(caseNotesClient.getCaseNote("A1234AA", olderCaseNoteId))
      .thenReturn(
        caseNote(
          olderCaseNoteId,
          text = "older",
          creationDateTime = older,
          amendments = listOf(amendment(amendedLatest)),
        ),
      )
    whenever(caseNotesClient.getCaseNote("A1234AA", middleCaseNoteId))
      .thenReturn(
        caseNote(
          middleCaseNoteId,
          text = "middle",
          creationDateTime = middle,
          amendments = listOf(amendment(oldestAmendment)),
        ),
      )
    whenever(caseNotesClient.getCaseNote("A1234AA", newerCaseNoteId))
      .thenReturn(caseNote(newerCaseNoteId, text = "newer", creationDateTime = newer))

    val request = SuggestedCaseNotesRequest(
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      sortField = "lastAmendedDate",
      sortOrder = "desc",
    )

    val response = service.buildSuggestedCaseNotes("A1234AA", request)

    assertThat(response.sortField).isEqualTo("lastAmendedDate")
    assertThat(response.sortOrder).isEqualTo("desc")
    assertThat(response.suggestedCaseNotes.map { it.caseNoteId }).containsExactly(olderCaseNoteId, newerCaseNoteId, middleCaseNoteId)
  }

  @Test
  fun `buildSuggestedCaseNotes returns empty list when no annotations exist`() {
    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(emptyList())

    val response = service.buildSuggestedCaseNotes("A1234AA", suggestedRequest())

    assertThat(response.suggestedCaseNotes).isEmpty()
  }

  @Test
  fun `buildSuggestedCaseNotes renders annotation content in annotatedCaseNote field`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(
        listOf(
          annotation(caseNoteId = caseNoteId, annotatedText = "became agitated", confidenceLevel = ConfidenceLevel.MEDIUM),
          annotation(caseNoteId = caseNoteId, annotatedText = "raised his voice", confidenceLevel = ConfidenceLevel.MEDIUM),
        ),
      )
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId))
      .thenReturn(caseNote(caseNoteId, text = "Prisoner became agitated and raised his voice."))

    val response = service.buildSuggestedCaseNotes("A1234AA", suggestedRequest())

    assertThat(response.suggestedCaseNotes).hasSize(1)
    val annotatedContent = response.suggestedCaseNotes.first().annotatedCaseNote
    assertThat(annotatedContent).isEqualTo(
      "Prisoner <span class=\"annotation-type\">became agitated</span> and <span class=\"annotation-type\">raised his voice</span>.",
    )
  }

  @Test
  fun `getCaseNotesWithAnnotations returns empty list when no matching annotations`() {
    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(emptyList())

    val result = service.getCaseNotesWithAnnotations("A1234AA", BehaviourType.RISKS_AND_TRIGGERS)

    assertThat(result).isEmpty()
    verify(caseNoteAnnotationRepository).findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS)
  }

  @Test
  fun `getCaseNotesWithAnnotations groups multiple annotations under one case note and fetches case note once`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    val annotationOne = annotation(caseNoteId = caseNoteId, annotatedText = "text 1")
    val annotationTwo = annotation(caseNoteId = caseNoteId, annotatedText = "text 2")

    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(listOf(annotationOne, annotationTwo))

    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId)).thenReturn(caseNote(caseNoteId))

    val result = service.getCaseNotesWithAnnotations("A1234AA", BehaviourType.RISKS_AND_TRIGGERS)

    assertThat(result).hasSize(1)
    assertThat(result.first().caseNote.caseNoteId).isEqualTo(caseNoteId)
    assertThat(result.first().annotations).hasSize(2)
    assertThat(result.first().annotations.map { it.annotatedText }).containsExactlyInAnyOrder("text 1", "text 2")

    verify(caseNoteAnnotationRepository).findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS)
    verify(caseNotesClient, times(1)).getCaseNote("A1234AA", caseNoteId)
  }

  @Test
  fun `getCaseNotesWithAnnotations retrieves one case note per unique case note id`() {
    val caseNoteIdOne = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    val caseNoteIdTwo = UUID.fromString("223e4567-e89b-12d3-a456-426614174000")

    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.PROTECTIVE_FACTORS))
      .thenReturn(
        listOf(
          annotation(
            caseNoteId = caseNoteIdOne,
            annotatedText = "one",
            behaviourType = BehaviourType.PROTECTIVE_FACTORS,
          ),
          annotation(
            caseNoteId = caseNoteIdTwo,
            annotatedText = "two",
            behaviourType = BehaviourType.PROTECTIVE_FACTORS,
          ),
        ),
      )

    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteIdOne)).thenReturn(caseNote(caseNoteIdOne))
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteIdTwo)).thenReturn(caseNote(caseNoteIdTwo))

    val result = service.getCaseNotesWithAnnotations("A1234AA", BehaviourType.PROTECTIVE_FACTORS)

    assertThat(result).hasSize(2)
    assertThat(result.map { it.caseNote.caseNoteId }).containsExactlyInAnyOrder(caseNoteIdOne, caseNoteIdTwo)
    verify(caseNotesClient, times(1)).getCaseNote("A1234AA", caseNoteIdOne)
    verify(caseNotesClient, times(1)).getCaseNote("A1234AA", caseNoteIdTwo)
  }

  @Test
  fun `composeCaseNoteAnnotation renders a single annotation`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("became agitated"),
    )

    val result = service.composeCaseNoteAnnotation(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner <span class=\"annotation-type\">became agitated</span> and later raised his voice.",
    )
  }

  @Test
  fun `composeCaseNoteAnnotation renders multiple annotations`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("became agitated", "raised his voice"),
    )

    val result = service.composeCaseNoteAnnotation(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner <span class=\"annotation-type\">became agitated</span> and later <span class=\"annotation-type\">raised his voice</span>.",
    )
  }

  @Test
  fun `composeCaseNoteAnnotation returns original text when no annotations`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = emptyList(),
    )

    val result = service.composeCaseNoteAnnotation(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(originalText)
  }

  @Test
  fun `composeCaseNoteAnnotation ignores null annotation text`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf(null, "raised his voice"),
    )

    val result = service.composeCaseNoteAnnotation(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner became agitated and later <span class=\"annotation-type\">raised his voice</span>.",
    )
  }

  @Test
  fun `composeCaseNoteAnnotation ignores blank annotation text`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("   ", "raised his voice"),
    )

    val result = service.composeCaseNoteAnnotation(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner became agitated and later <span class=\"annotation-type\">raised his voice</span>.",
    )
  }

  @Test
  fun `composeCaseNoteAnnotation preserves original content outside annotations`() {
    val originalText = "On review, prisoner became agitated, then settled down after staff support."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("became agitated"),
    )

    val result = service.composeCaseNoteAnnotation(caseNoteWithAnnotations)

    assertThat(result).startsWith("On review, prisoner ")
    assertThat(result).contains("<span class=\"annotation-type\">became agitated</span>")
    assertThat(result).endsWith(", then settled down after staff support.")
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
  ) = CaseNoteAnnotation(
    id = UUID.randomUUID(),
    requestId = UUID.randomUUID(),
    prisonerNumber = "A1234AA",
    caseNoteId = caseNoteId,
    promptKey = "case-note-analysis",
    promptVersion = 3,
    behaviourType = behaviourType,
    confidenceLevel = confidenceLevel,
    annotatedText = annotatedText,
    createdDate = LocalDateTime.now(),
  )

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

  private fun amendment(creationDateTime: LocalDateTime) = CaseNoteAmendment(
    creationDateTime = creationDateTime,
    authorUserName = "amender.username",
    authorName = "Amender Name",
    authorUserId = "USER2",
    additionalNoteText = "extra detail",
    id = UUID.randomUUID(),
  )

  private fun suggestedRequest() = SuggestedCaseNotesRequest(
    behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
    sortField = "relevance",
    sortOrder = "desc",
  )

  private fun setupThreeCaseNotesForSorting(): SortingCaseNotesSetup {
    val olderCaseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    val middleCaseNoteId = UUID.fromString("173e4567-e89b-12d3-a456-426614174000")
    val newerCaseNoteId = UUID.fromString("223e4567-e89b-12d3-a456-426614174000")
    val older = LocalDateTime.of(2025, 1, 1, 9, 0)
    val middle = LocalDateTime.of(2025, 3, 1, 9, 0)
    val newer = LocalDateTime.of(2025, 6, 1, 9, 0)

    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(
        listOf(
          annotation(caseNoteId = olderCaseNoteId, annotatedText = "older"),
          annotation(caseNoteId = middleCaseNoteId, annotatedText = "middle"),
          annotation(caseNoteId = newerCaseNoteId, annotatedText = "newer"),
        ),
      )
    whenever(caseNotesClient.getCaseNote("A1234AA", olderCaseNoteId))
      .thenReturn(caseNote(olderCaseNoteId, text = "older", creationDateTime = older))
    whenever(caseNotesClient.getCaseNote("A1234AA", middleCaseNoteId))
      .thenReturn(caseNote(middleCaseNoteId, text = "middle", creationDateTime = middle))
    whenever(caseNotesClient.getCaseNote("A1234AA", newerCaseNoteId))
      .thenReturn(caseNote(newerCaseNoteId, text = "newer", creationDateTime = newer))

    return SortingCaseNotesSetup(
      olderCaseNoteId = olderCaseNoteId,
      middleCaseNoteId = middleCaseNoteId,
      newerCaseNoteId = newerCaseNoteId,
    )
  }

  private data class SortingCaseNotesSetup(
    val olderCaseNoteId: UUID,
    val middleCaseNoteId: UUID,
    val newerCaseNoteId: UUID,
  )

  private fun caseNoteWithAnnotations(
    text: String,
    annotationTexts: List<String?>,
  ): CaseNoteWithAnnotations {
    val caseNoteId = UUID.fromString("323e4567-e89b-12d3-a456-426614174000")

    return CaseNoteWithAnnotations(
      caseNote = caseNote(caseNoteId = caseNoteId, text = text),
      annotations = annotationTexts.map { annotatedText -> annotationSummary(caseNoteId, annotatedText) },
    )
  }

  private fun annotationSummary(caseNoteId: UUID, annotatedText: String?) = CaseNoteAnnotationSummary(
    id = UUID.randomUUID(),
    requestId = UUID.randomUUID(),
    prisonerNumber = "A1234AA",
    caseNoteId = caseNoteId,
    promptKey = "case-note-analysis",
    promptVersion = 3,
    behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
    confidenceLevel = ConfidenceLevel.HIGH,
    annotatedText = annotatedText,
    createdDate = LocalDateTime.now(),
  )

  private fun testResponse() = JdaDequeueResponse(
    requestId = UUID.randomUUID(),
    correlationId = UUID.randomUUID(),
    prompt = JdaPrompt(
      key = "case-note-analysis",
      version = 3,
    ),
    status = JdaDequeueResponseStatus.SUCCEEDED,
    responseData = listOf(
      JdaDequeueResponseData(
        caseNoteId = UUID.randomUUID(),
        confidenceLevel = ConfidenceLevel.HIGH,
        justifyingSpans = listOf(
          JustifyingSpan(
            text = "annotated text 1",
            justifies = BehaviourType.PROTECTIVE_FACTORS,
          ),
          JustifyingSpan(
            text = "annotated text 2",
            justifies = BehaviourType.RISKS_AND_TRIGGERS,
          ),
          JustifyingSpan(
            text = "annotated text 3",
            justifies = BehaviourType.USUAL_BEHAVIOUR_PRESENTATION,
          ),
          JustifyingSpan(
            text = "annotated text 4",
            justifies = BehaviourType.PROTECTIVE_FACTORS,
          ),
        ),
      ),
    ),
    metaData = JdaDequeueResponseMetadata(
      requestType = JdaRequestType.ASYNC,
      completedAt = LocalDateTime.now(),
      completionMs = 1200,
    ),
  )

  private fun testJdaRequestResponse(requestId: UUID = UUID.randomUUID()): JdaRequestResponse = JdaRequestResponse(
    requestId = requestId,
    correlationId = UUID.randomUUID(),
    prompt = JdaPrompt(
      key = "case-note-analysis",
      version = 0,
    ),
    status = JdaRequestStatus.SUCCEEDED,
    responseData = listOf(
      JdaDequeueResponseData(
        caseNoteId = UUID.randomUUID(),
        confidenceLevel = ConfidenceLevel.HIGH,
        justifyingSpans = listOf(
          JustifyingSpan(
            text = "annotated text 1",
            justifies = BehaviourType.PROTECTIVE_FACTORS,
          ),
          JustifyingSpan(
            text = "annotated text 2",
            justifies = BehaviourType.RISKS_AND_TRIGGERS,
          ),
          JustifyingSpan(
            text = "annotated text 3",
            justifies = BehaviourType.USUAL_BEHAVIOUR_PRESENTATION,
          ),
          JustifyingSpan(
            text = "annotated text 4",
            justifies = BehaviourType.PROTECTIVE_FACTORS,
          ),
        ),
      ),
    ),
    metaData = JdaMetadata(
      requestType = JdaRequestType.SYNC,
      submittedAt = java.time.OffsetDateTime.now(),
    ),
  )
}
