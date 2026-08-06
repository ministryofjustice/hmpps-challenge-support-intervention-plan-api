package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda.JdaClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestType
import java.time.OffsetDateTime
import java.util.UUID

class CaseNoteAnnotationsServiceTest {
  private val jdaClient = mock<JdaClient>()
  private val caseNoteAnnotationsPersistenceService = mock<CaseNoteAnnotationsPersistenceService>()
  private val service = CaseNoteAnnotationsService(jdaClient, caseNoteAnnotationsPersistenceService)

  @Test
  fun `getCaseNoteAnnotationsFromQueue polls until dequeue returns null`() {
    whenever(jdaClient.getCaseNoteAnnotationsFromQueue())
      .thenReturn(testResponse())
      .thenReturn(testResponse())
      .thenReturn(null)

    service.getCaseNoteAnnotationsFromQueue()

    verify(jdaClient, times(3)).getCaseNoteAnnotationsFromQueue()
    verify(caseNoteAnnotationsPersistenceService, times(2)).persistCaseNoteAnnotations(any())
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
      service.getCaseNoteAnnotationsFromQueue()
    }

    verify(caseNoteAnnotationsPersistenceService, never()).persistCaseNoteAnnotations(any())
  }

  @Test
  fun `getCaseNoteAnnotationsFromQueue continues draining when persisting an item fails`() {
    val firstResponse = testResponse()
    val secondResponse = testResponse()
    val thirdResponse = testResponse()

    whenever(jdaClient.getCaseNoteAnnotationsFromQueue())
      .thenReturn(firstResponse)
      .thenReturn(secondResponse)
      .thenReturn(thirdResponse)
      .thenReturn(null)

    doThrow(RuntimeException("Something went wrong"))
      .whenever(caseNoteAnnotationsPersistenceService).persistCaseNoteAnnotations(secondResponse)

    service.getCaseNoteAnnotationsFromQueue()

    verify(jdaClient, times(4)).getCaseNoteAnnotationsFromQueue()
    verify(caseNoteAnnotationsPersistenceService).persistCaseNoteAnnotations(firstResponse)
    verify(caseNoteAnnotationsPersistenceService).persistCaseNoteAnnotations(secondResponse)
    verify(caseNoteAnnotationsPersistenceService).persistCaseNoteAnnotations(thirdResponse)
  }

  private fun testResponse() = JdaDequeueResponse(
    requestId = UUID.randomUUID(),
    correlationId = UUID.randomUUID().toString(),
    prompt = JdaPrompt.caseNoteAnalysis(),
    status = JdaDequeueResponseStatus.SUCCEEDED,
    responseData = JdaDequeueResponseData(
      todo = "todo",
    ),
    metadata = JdaDequeueResponseMetadata(
      requestType = JdaRequestType.ASYNC,
      completedAt = OffsetDateTime.now(),
      completionMs = 1200,
    ),
  )
}
