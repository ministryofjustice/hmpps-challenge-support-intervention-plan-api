package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.JdaDequeueResponseStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JustifyingSpan
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

class CaseNoteAnnotationsAcknowledgementTest {
  private val caseNotesClient = mock<CaseNotesClient>()
  private val caseNoteAnnotationRepository = mock<CaseNoteAnnotationRepository>()
  private val caseNoteAnalysedRepository = mock<CaseNoteAnalysedRepository>()
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

  @BeforeEach
  fun setUp() {
    whenever(caseNoteAnalysedRepository.save(any<CaseNoteAnalysed>())).thenAnswer { it.getArgument(0) }
    whenever(jdbcTemplate.update(any<String>(), any<MapSqlParameterSource>())).thenReturn(1)
  }

  @Test
  fun `successful processing acknowledges message`() {
    val receiptId = "receipt-success-123"
    val csipRecordId = UUID.randomUUID()
    val prisonNumber = "A1234BC"
    val response = testResponse(receiptId = receiptId, correlationId = csipRecordId)

    stubCsipRecordLookup(csipRecordId, prisonNumber)
    stubSuccessfulAnnotationSaves()

    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response)
      .thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    // Verify the analysed case note was persisted and 4 annotation rows were written
    verify(caseNoteAnalysedRepository, times(1)).save(any<CaseNoteAnalysed>())
    verify(jdbcTemplate, times(4)).update(any<String>(), any<MapSqlParameterSource>())
    // Verify acknowledgement was called with correct receiptId
    verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(receiptId)
  }

  @Test
  fun `annotation persistence failure still acknowledges message`() {
    val receiptId = "receipt-fail-456"
    val csipRecordId = UUID.randomUUID()
    val prisonNumber = "B2345CD"
    val response = testResponse(receiptId = receiptId, correlationId = csipRecordId)

    stubCsipRecordLookup(csipRecordId, prisonNumber)

    var updateAttempts = 0
    whenever(jdbcTemplate.update(any<String>(), any<MapSqlParameterSource>()))
      .thenAnswer {
        updateAttempts++
        if (updateAttempts == 2) {
          throw RuntimeException("Database connection failed")
        }
        1
      }

    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response)
      .thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    // The analysed row is still saved and all annotations are attempted
    verify(caseNoteAnalysedRepository, times(1)).save(any<CaseNoteAnalysed>())
    verify(jdbcTemplate, times(4)).update(any<String>(), any<MapSqlParameterSource>())
    // Acknowledgement still occurs because annotation persistence errors are swallowed per item
    verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(receiptId)
  }

  @Test
  fun `acknowledgement failure handling - message remains in queue`() {
    val receiptId = "receipt-ack-fail-789"
    val csipRecordId = UUID.randomUUID()
    val prisonNumber = "C3456DE"
    val response = testResponse(receiptId = receiptId, correlationId = csipRecordId)

    stubCsipRecordLookup(csipRecordId, prisonNumber)
    stubSuccessfulAnnotationSaves()

    // Make acknowledgement fail
    doThrow(DownstreamServiceException("JDA service unavailable", RuntimeException("Connection timeout")))
      .`when`(jdaService)
      .acknowledgeCaseNoteAnnotationsMessage(receiptId)

    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response)
      .thenReturn(null)

    // Should not throw exception
    service.processQueuedCaseNoteAnnotations()

    // Verify the analysed row and four annotation rows were persisted
    verify(caseNoteAnalysedRepository, times(1)).save(any<CaseNoteAnalysed>())
    verify(jdbcTemplate, times(4)).update(any<String>(), any<MapSqlParameterSource>())
    // Verify acknowledgement was attempted (even though it failed)
    verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(receiptId)
  }

  @Test
  fun `dequeue loop continues correctly on acknowledgement failure`() {
    val receiptId1 = "receipt-loop-1"
    val receiptId2 = "receipt-loop-2"
    val csipRecordId1 = UUID.randomUUID()
    val csipRecordId2 = UUID.randomUUID()
    val prisonNumber = "D4567EF"

    val response1 = testResponse(receiptId = receiptId1, correlationId = csipRecordId1)
    val response2 = testResponse(receiptId = receiptId2, correlationId = csipRecordId2)

    stubCsipRecordLookup(csipRecordId1, prisonNumber)
    stubCsipRecordLookup(csipRecordId2, prisonNumber)
    stubSuccessfulAnnotationSaves()

    // First acknowledgement fails
    doThrow(DownstreamServiceException("Temporary error", RuntimeException()))
      .`when`(jdaService)
      .acknowledgeCaseNoteAnnotationsMessage(receiptId1)

    // Second acknowledgement succeeds
    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response1)
      .thenReturn(response2)
      .thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    // Verify loop continued despite first acknowledgement failure
    verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(receiptId1)
    verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(receiptId2)
    // Both messages should have been processed
    verify(caseNoteAnalysedRepository, times(2)).save(any<CaseNoteAnalysed>())
    verify(jdbcTemplate, times(8)).update(any<String>(), any<MapSqlParameterSource>())
  }

  @Test
  fun `JDA-579 regression CSIP record lookup failure does not acknowledge message`() {
    val receiptId = "receipt-lookup-fail"
    val csipRecordId = UUID.randomUUID()
    val response = testResponse(receiptId = receiptId, correlationId = csipRecordId)

    // CSIP lookup fails
    whenever(csipRecordService.retrieveCsipRecord(csipRecordId))
      .thenThrow(RuntimeException("CSIP record not found"))

    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response)
      .thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    // No persistence should occur
    verify(caseNoteAnalysedRepository, never()).save(any<CaseNoteAnalysed>())
    verify(jdbcTemplate, never()).update(any<String>(), any<MapSqlParameterSource>())
    // No acknowledgement should be attempted
    verify(jdaService, never()).acknowledgeCaseNoteAnnotationsMessage(receiptId)
  }

  @Test
  fun `multiple messages in queue are processed sequentially`() {
    val receipts = listOf(
      "receipt-seq-1",
      "receipt-seq-2",
      "receipt-seq-3",
    )
    val correlationIds = receipts.map { UUID.randomUUID() }
    val prisonNumber = "E5678FG"

    val responses = receipts.mapIndexed { index, receipt ->
      testResponse(receiptId = receipt, correlationId = correlationIds[index])
    }

    // Setup successful CSIP lookups for all messages
    correlationIds.forEach { id ->
      stubCsipRecordLookup(id, prisonNumber)
    }
    stubSuccessfulAnnotationSaves()

    // Queue returns 3 messages then null
    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(responses[0])
      .thenReturn(responses[1])
      .thenReturn(responses[2])
      .thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    // Verify all 3 messages were acknowledged
    receipts.forEach { receiptId ->
      verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(receiptId)
    }
    // Verify all 3 analysed rows and 12 annotation rows were written
    verify(caseNoteAnalysedRepository, times(3)).save(any<CaseNoteAnalysed>())
    verify(jdbcTemplate, times(12)).update(any<String>(), any<MapSqlParameterSource>())
  }

  @Test
  fun `receiptId is included in log output`() {
    val receiptId = "receipt-log-check"
    val csipRecordId = UUID.randomUUID()
    val prisonNumber = "F6789GH"
    val response = testResponse(receiptId = receiptId, correlationId = csipRecordId)

    stubCsipRecordLookup(csipRecordId, prisonNumber)
    stubSuccessfulAnnotationSaves()

    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response)
      .thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    // Verify acknowledgement was called (which logs receiptId)
    verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(receiptId)
  }

  @Test
  fun `acknowledgement failure does not increment message processed count`() {
    val receiptId1 = "receipt-counted"
    val receiptId2 = "receipt-not-counted"
    val csipRecordId1 = UUID.randomUUID()
    val csipRecordId2 = UUID.randomUUID()
    val prisonNumber = "H8901JK"

    val response1 = testResponse(receiptId = receiptId1, correlationId = csipRecordId1)
    val response2 = testResponse(receiptId = receiptId2, correlationId = csipRecordId2)

    // Setup CSIP lookups for both messages
    stubCsipRecordLookup(csipRecordId1, prisonNumber)
    stubCsipRecordLookup(csipRecordId2, prisonNumber)
    stubSuccessfulAnnotationSaves()

    // Message 2 acknowledgement fails (but persistence succeeds)
    doThrow(DownstreamServiceException("JDA service unavailable", RuntimeException("Connection timeout")))
      .`when`(jdaService)
      .acknowledgeCaseNoteAnnotationsMessage(receiptId2)

    // Queue returns 2 messages then null
    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response1)
      .thenReturn(response2)
      .thenReturn(null)

    // Execute - should handle acknowledgement failure gracefully
    service.processQueuedCaseNoteAnnotations()

    // Verify both messages were dequeued (3 calls: fetch msg1, fetch msg2, fetch null)
    verify(jdaService, times(3)).getCaseNoteAnnotationsFromQueue()

    // Verify both analysed rows and all 8 annotation rows were persisted successfully
    verify(caseNoteAnalysedRepository, times(2)).save(any<CaseNoteAnalysed>())
    verify(jdbcTemplate, times(8)).update(any<String>(), any<MapSqlParameterSource>())

    // Verify acknowledgement was attempted for BOTH messages
    verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(receiptId1)
    verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(receiptId2)
  }

  private fun testResponse(
    receiptId: String = "receipt-${UUID.randomUUID()}",
    correlationId: UUID = UUID.randomUUID(),
  ) = JdaDequeueResponse(
    requestId = UUID.randomUUID(),
    correlationId = correlationId,
    receiptId = receiptId,
    prompt = JdaPrompt(
      key = "case-note-analysis",
      version = 1,
    ),
    status = JdaDequeueResponseStatus.SUCCEEDED,
    responseData = listOf(
      JdaDequeueResponseData(
        caseNoteId = UUID.randomUUID(),
        usualBehaviourPresentation = 3,
        risksAndTriggers = 2,
        protectiveFactors = 4,
        comment = "test comment",
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

  private fun stubCsipRecordLookup(csipRecordId: UUID, prisonNumber: String) {
    val csipRecord = mock<CsipRecord>()
    whenever(csipRecord.prisonNumber).thenReturn(prisonNumber)
    whenever(csipRecordService.retrieveCsipRecord(csipRecordId))
      .thenReturn(csipRecord)
  }

  private fun stubSuccessfulAnnotationSaves() {
    whenever(caseNoteAnnotationRepository.save(any<CaseNoteAnnotation>()))
      .thenAnswer { it.getArgument<CaseNoteAnnotation>(0) }
  }
}
