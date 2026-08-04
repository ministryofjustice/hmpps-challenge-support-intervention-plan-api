package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
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
import java.time.Instant
import java.util.UUID

class CaseNoteAnnotationsServiceTest {
  private val jdaClient = mock<JdaClient>()
  private val service = CaseNoteAnnotationsService(jdaClient)

  @Test
  fun `getCaseNoteAnnotationsFromQueue polls when dequeue returns null`() {
    whenever(jdaClient.getCaseNoteAnnotationsFromQueue())
      .thenReturn(testResponse())
      .thenReturn(testResponse())
      .thenReturn(null)

    service.getCaseNoteAnnotationsFromQueue()

    verify(jdaClient, times(3)).getCaseNoteAnnotationsFromQueue()
  }

  @Test
  fun `getCaseNoteAnnotationsFromQueue rethrows downstream failures`() {
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

    verify(jdaClient).getCaseNoteAnnotationsFromQueue()
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
      completedAt = Instant.now(),
      completionMs = 1200,
    ),
  )
}
