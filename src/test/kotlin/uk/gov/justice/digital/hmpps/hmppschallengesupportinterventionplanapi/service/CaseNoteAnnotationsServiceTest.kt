package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda.JdaClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.JdaDequeueResponseStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException
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
import java.time.LocalDateTime
import java.util.UUID

class CaseNoteAnnotationsServiceTest {
  private val jdaClient = mock<JdaClient>()
  private val caseNoteAnnotationRepository = mock<CaseNoteAnnotationRepository>()
  private val csipRecordService = mock<CsipRecordService>()
  private val service = CaseNoteAnnotationsService(jdaClient, caseNoteAnnotationRepository, csipRecordService)

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
      metadata = JdaDequeueResponseMetadata(
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

  private fun stubCsipRecordLookup(prisonNumber: String = "A1234BC") {
    val csipRecord = mock<CsipRecord>()
    whenever(csipRecord.prisonNumber).thenReturn(prisonNumber)
    whenever(csipRecordService.retrieveCsipRecord(any())).thenReturn(csipRecord)
  }

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
    metadata = JdaDequeueResponseMetadata(
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
    metadata = JdaMetadata(
      requestType = JdaRequestType.SYNC,
      submittedAt = java.time.OffsetDateTime.now(),
    ),
  )
}
