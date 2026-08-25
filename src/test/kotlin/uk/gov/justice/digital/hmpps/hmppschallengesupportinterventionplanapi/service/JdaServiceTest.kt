package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda.JdaClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipAssistConfig
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.CaseNoteAnalysisItem
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JustifyingSpan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

class JdaServiceTest {

  private val caseNotesService = mock<CaseNotesService>()
  private val jdaClient = mock<JdaClient>()
  private val caseNoteAnnotationsService = mock<CaseNoteAnnotationsService>()
  private val csipRecordService = mock<CsipRecordService>()
  private val csipAssistConfig = mock<CsipAssistConfig>()

  private val service =
    JdaService(
      caseNotesService,
      jdaClient,
      caseNoteAnnotationsService,
      csipRecordService,
      csipAssistConfig,
      "case-note-analysis",
      1,
      true,
    )

  private val serviceWithFeatureDisabled =
    JdaService(
      caseNotesService,
      jdaClient,
      caseNoteAnnotationsService,
      csipRecordService,
      csipAssistConfig,
      "case-note-analysis",
      1,
      false,
    )

  private val offenderIdentifier = "A1234AA"
  private val prisonCode = "NMI"
  private val correlationId = UUID.randomUUID().toString()

  @Test
  fun `submitCaseNotesForAnalysis submits request and persists annotations when feature enabled and prison active`() {
    whenever(
      csipAssistConfig.isActivePrison(prisonCode),
    ).thenReturn(true)

    val response = testCaseNotesResponse()

    whenever(
      caseNotesService.getCaseNotes(any(), any()),
    ).thenReturn(response)

    val correlationUuid = UUID.fromString(correlationId)
    val jdaResponse = testJdaRequestResponse()
    whenever(jdaClient.submitRequest(any<JdaRequest<List<CaseNoteAnalysisItem>>>())).thenReturn(jdaResponse)

    val csipRecord = mock<CsipRecord>()
    whenever(csipRecord.prisonNumber).thenReturn("A1234BC")
    whenever(csipRecordService.retrieveCsipRecord(correlationUuid)).thenReturn(csipRecord)

    service.submitCaseNotesForAnalysis(
      offenderIdentifier = offenderIdentifier,
      prisonCode = prisonCode,
      correlationId = correlationId,
    )

    verify(csipAssistConfig)
      .isActivePrison(prisonCode)

    val caseNotesLookupRequestCaptor = argumentCaptor<CaseNotesLookupRequest>()
    verify(caseNotesService)
      .getCaseNotes(caseNotesLookupRequestCaptor.capture(), any())

    assertThat(caseNotesLookupRequestCaptor.firstValue.offenderIdentifier)
      .isEqualTo(offenderIdentifier)
    assertThat(caseNotesLookupRequestCaptor.firstValue.includeSensitive)
      .isTrue()

    val requestCaptor =
      argumentCaptor<JdaRequest<List<CaseNoteAnalysisItem>>>()

    verify(jdaClient)
      .submitRequest(requestCaptor.capture())

    assertThat(requestCaptor.firstValue.correlationId)
      .isEqualTo(correlationId)

    assertThat(requestCaptor.firstValue.prompt.key)
      .isEqualTo("case-note-analysis")

    assertThat(requestCaptor.firstValue.prompt.version)
      .isEqualTo(1)

    assertThat(requestCaptor.firstValue.requestData)
      .hasSize(1)

    assertThat(requestCaptor.firstValue.requestData.first().caseNoteText)
      .isEqualTo("Prisoner became agitated")

    assertThat(requestCaptor.firstValue.requestData.first().caseNoteId)
      .isEqualTo("f4ee95d0-49a4-46a2-a485-b8f26f089170")

    // Verify annotations were persisted
    verify(caseNoteAnnotationsService)
      .persistSynchronousAnnotations(jdaResponse, "A1234BC")
  }

  @Test
  fun `submitCaseNotesForAnalysis does nothing when feature flag disabled`() {
    serviceWithFeatureDisabled.submitCaseNotesForAnalysis(
      offenderIdentifier = offenderIdentifier,
      prisonCode = prisonCode,
      correlationId = correlationId,
    )

    verify(csipAssistConfig, never())
      .isActivePrison(any())

    verifyNoInteractions(caseNotesService)

    verifyNoInteractions(jdaClient)

    verifyNoInteractions(caseNoteAnnotationsService)
  }

  @Test
  fun `submitCaseNotesForAnalysis does nothing when prison is not active`() {
    whenever(
      csipAssistConfig.isActivePrison(prisonCode),
    ).thenReturn(false)

    service.submitCaseNotesForAnalysis(
      offenderIdentifier = offenderIdentifier,
      prisonCode = prisonCode,
      correlationId = correlationId,
    )

    verify(csipAssistConfig)
      .isActivePrison(prisonCode)

    verifyNoInteractions(caseNotesService)

    verifyNoInteractions(jdaClient)

    verifyNoInteractions(caseNoteAnnotationsService)
  }

  @Test
  fun `submitCaseNotesForAnalysis continues when annotation persistence fails`() {
    whenever(
      csipAssistConfig.isActivePrison(prisonCode),
    ).thenReturn(true)

    val response = testCaseNotesResponse()

    whenever(
      caseNotesService.getCaseNotes(any(), any()),
    ).thenReturn(response)

    val correlationUuid = UUID.fromString(correlationId)
    val jdaResponse = testJdaRequestResponse()
    whenever(jdaClient.submitRequest(any<JdaRequest<List<CaseNoteAnalysisItem>>>())).thenReturn(jdaResponse)

    val csipRecord = mock<CsipRecord>()
    whenever(csipRecord.prisonNumber).thenReturn("A1234BC")
    whenever(csipRecordService.retrieveCsipRecord(correlationUuid)).thenReturn(csipRecord)

    // Make annotation persistence fail
    whenever(caseNoteAnnotationsService.persistSynchronousAnnotations(any(), any()))
      .thenThrow(RuntimeException("Persistence failed"))

    // Should not throw, should continue
    service.submitCaseNotesForAnalysis(
      offenderIdentifier = offenderIdentifier,
      prisonCode = prisonCode,
      correlationId = correlationId,
    )

    verify(caseNoteAnnotationsService)
      .persistSynchronousAnnotations(jdaResponse, "A1234BC")
  }

  private fun testCaseNotesResponse(
    caseNote: CaseNote = testCaseNote(),
  ) = CaseNotesResponse(
    content = listOf(caseNote),
    hasCaseNotes = true,
    metadata = CaseNotesMetadata(
      totalElements = 1,
      page = 0,
      size = 1,
    ),
  )

  private fun testCaseNote() = CaseNote(
    caseNoteId = UUID.fromString("f4ee95d0-49a4-46a2-a485-b8f26f089170"),
    offenderIdentifier = "A1234AA",
    type = "GEN",
    typeDescription = "General",
    subType = "OBS",
    subTypeDescription = "Observation",
    creationDateTime = LocalDateTime.now(),
    occurrenceDateTime = LocalDateTime.now(),
    authorName = "Test User",
    authorUserId = "USER1",
    authorUsername = "testuser",
    text = "Prisoner became agitated",
    locationId = "MDI",
    sensitive = false,
    amendments = emptyList(),
  )

  private fun testJdaRequestResponse(): JdaRequestResponse {
    val requestId = UUID.randomUUID()
    val correlationUuid = UUID.fromString(correlationId)
    return JdaRequestResponse(
      requestId = requestId,
      correlationId = correlationUuid,
      prompt = JdaPrompt(
        key = "case-note-analysis",
        version = 0,
      ),
      status = JdaRequestStatus.SUCCEEDED,
      responseData = listOf(
        JdaDequeueResponseData(
          caseNoteId = UUID.fromString("f4ee95d0-49a4-46a2-a485-b8f26f089170"),
          confidenceLevel = ConfidenceLevel.HIGH,
          justifyingSpans = listOf(
            JustifyingSpan(
              text = "agitated",
              justifies = uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType.RISKS_AND_TRIGGERS,
            ),
          ),
        ),
      ),
      metadata = JdaMetadata(
        requestType = JdaRequestType.SYNC,
        submittedAt = OffsetDateTime.now(),
      ),
    )
  }
}
