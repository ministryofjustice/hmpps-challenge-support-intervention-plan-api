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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNoteAmendment
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.manageusers.UserDetails
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
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
  private val testUsername = "TEST_USER"
  private val caseNotesClient = mock<CaseNotesClient>()
  private val caseNoteAnalysedRepository = mock<CaseNoteAnalysedRepository>()
  private val caseNoteAnnotationRepository = mock<CaseNoteAnnotationRepository>()
  private val jdbcTemplate = mock<NamedParameterJdbcTemplate>()
  private val csipRecordService = mock<CsipRecordService>()
  private val caseNotesService = CaseNotesService(caseNotesClient)
  private val jdaService = mock<JdaService>()
  private val personSummaryService = mock<PersonSummaryService>()
  private val userService = mock<UserService>()
  private val service = CaseNoteAnnotationsService(
    caseNotesService,
    jdaService,
    caseNoteAnalysedRepository,
    caseNoteAnnotationRepository,
    jdbcTemplate,
    personSummaryService,
    csipRecordService,
    userService,
    Duration.ofSeconds(30),
  )
  private val referralId = UUID.fromString("9ec1ca0c-0d92-4ae4-b307-0a57759ac52e")

  @BeforeEach
  fun setUp() {
    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("TEST_USER", "password")

    whenever(caseNoteAnalysedRepository.save(any<CaseNoteAnalysed>())).thenAnswer { it.getArgument(0) }
    whenever(caseNoteAnalysedRepository.saveAndFlush(any<CaseNoteAnalysed>())).thenAnswer { it.getArgument(0) }
    whenever(jdbcTemplate.update(any<String>(), any<MapSqlParameterSource>())).thenReturn(1)
    whenever(userService.getUserDetails(any())).thenReturn(
      UserDetails(
        username = "TEST_USER",
        active = true,
        name = "Test User",
        authSource = "nomis",
        userId = "123",
        uuid = UUID.randomUUID(),
        activeCaseLoadId = "LEI",
      ),
    )
    whenever(userService.getUserRoles(any())).thenReturn(emptyList())
  }

  @Test
  fun `processQueuedCaseNoteAnnotations handles an empty queue gracefully`() {
    whenever(jdaService.getCaseNoteAnnotationsFromQueue()).thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    verify(jdaService, times(1)).getCaseNoteAnnotationsFromQueue()
    verify(caseNoteAnalysedRepository, never()).save(any())
    verify(caseNoteAnnotationRepository, never()).save(any())
    verify(jdbcTemplate, never()).update(any<String>(), any<MapSqlParameterSource>())
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
  fun `processQueuedCaseNoteAnnotations skips responses with no response data`() {
    val responseWithoutData = testResponse(responseData = null)

    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(responseWithoutData)
      .thenReturn(null)
    stubCsipRecordLookup()

    service.processQueuedCaseNoteAnnotations()

    verify(jdaService, times(2)).getCaseNoteAnnotationsFromQueue()
    verify(csipRecordService, times(1)).retrieveCsipRecord(any())
    verify(caseNoteAnalysedRepository, never()).save(any())
    verify(jdbcTemplate, never()).update(any<String>(), any<MapSqlParameterSource>())
  }

  @Test
  fun `processQueuedCaseNoteAnnotations skips empty response data and still acknowledges`() {
    val responseWithoutData = testResponse(responseData = emptyList())

    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(responseWithoutData)
      .thenReturn(null)
    stubCsipRecordLookup()

    service.processQueuedCaseNoteAnnotations()

    verify(jdaService, times(2)).getCaseNoteAnnotationsFromQueue()
    verify(csipRecordService, times(1)).retrieveCsipRecord(any())
    verify(caseNoteAnalysedRepository, never()).save(any())
    verify(jdbcTemplate, never()).update(any<String>(), any<MapSqlParameterSource>())
    verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(responseWithoutData.receiptId)
  }

  @Test
  fun `persistSynchronousAnnotations persists analysed rows and annotations`() {
    val requestId = UUID.randomUUID()
    val prisonerNumber = "A1234BC"
    val response = testJdaRequestResponse(requestId)

    service.persistSynchronousAnnotations(response, prisonerNumber)

    val analysedCaptor = argumentCaptor<CaseNoteAnalysed>()
    verify(caseNoteAnalysedRepository, times(1)).saveAndFlush(analysedCaptor.capture())
    assertThat(analysedCaptor.firstValue.prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(analysedCaptor.firstValue.protectiveFactorsRelevancy).isEqualTo(4)

    verify(caseNoteAnnotationRepository, never()).deleteByCaseNotesAnalysedId(any())
    verify(jdbcTemplate, times(4)).update(any<String>(), any<MapSqlParameterSource>())
  }

  @Test
  fun `persistSynchronousAnnotations updates existing analysed row for matching investigation and case note even when requestId differs`() {
    val requestId = UUID.randomUUID()
    val existingRequestId = UUID.randomUUID()
    val caseNoteId = UUID.randomUUID()
    val existingAnalysedId = UUID.randomUUID()
    val prisonerNumber = "A1234BC"
    val response = testJdaRequestResponse(
      requestId = requestId,
      correlationId = referralId,
      responseData = listOf(
        JdaDequeueResponseData(
          caseNoteId = caseNoteId,
          usualBehaviourPresentation = 1,
          risksAndTriggers = 4,
          protectiveFactors = 2,
          justifyingSpans = listOf(
            JustifyingSpan(text = "replacement annotation", justifies = BehaviourType.RISKS_AND_TRIGGERS),
          ),
        ),
      ),
    )

    whenever(
      caseNoteAnalysedRepository.findByInvestigationIdAndCaseNoteId(
        referralId,
        caseNoteId,
      ),
    ).thenReturn(
      CaseNoteAnalysed(
        id = existingAnalysedId,
        requestId = existingRequestId,
        investigationId = referralId,
        prisonerNumber = prisonerNumber,
        caseNoteId = caseNoteId,
        promptKey = "old-prompt",
        promptVersion = 1,
        usualBehaviourRelevancy = 4,
        risksAndTriggersRelevancy = 1,
        protectiveFactorsRelevancy = 0,
      ),
    )

    service.persistSynchronousAnnotations(response, prisonerNumber)

    verify(caseNoteAnnotationRepository).deleteByCaseNotesAnalysedId(existingAnalysedId)

    val analysedCaptor = argumentCaptor<CaseNoteAnalysed>()
    verify(caseNoteAnalysedRepository).saveAndFlush(analysedCaptor.capture())
    assertThat(analysedCaptor.firstValue.id).isEqualTo(existingAnalysedId)
    assertThat(analysedCaptor.firstValue.requestId).isEqualTo(requestId)
    assertThat(analysedCaptor.firstValue.promptKey).isEqualTo("case-note-analysis")
    assertThat(analysedCaptor.firstValue.promptVersion).isEqualTo(3)
    assertThat(analysedCaptor.firstValue.usualBehaviourRelevancy).isEqualTo(1)
    assertThat(analysedCaptor.firstValue.risksAndTriggersRelevancy).isEqualTo(4)
    assertThat(analysedCaptor.firstValue.protectiveFactorsRelevancy).isEqualTo(2)
    verify(caseNoteAnalysedRepository).findByInvestigationIdAndCaseNoteId(referralId, caseNoteId)
    verify(jdbcTemplate, times(1)).update(any<String>(), any<MapSqlParameterSource>())
  }

  @Test
  fun `persistSynchronousAnnotations rethrows failures so the transaction can roll back`() {
    val requestId = UUID.randomUUID()
    val caseNoteId = UUID.randomUUID()
    val existingAnalysedId = UUID.randomUUID()
    val response = testJdaRequestResponse(
      requestId = requestId,
      correlationId = referralId,
      responseData = listOf(
        JdaDequeueResponseData(
          caseNoteId = caseNoteId,
          justifyingSpans = listOf(
            JustifyingSpan(text = "replacement annotation", justifies = BehaviourType.RISKS_AND_TRIGGERS),
          ),
        ),
      ),
    )

    whenever(
      caseNoteAnalysedRepository.findByInvestigationIdAndCaseNoteId(
        referralId,
        caseNoteId,
      ),
    ).thenReturn(
      CaseNoteAnalysed(
        id = existingAnalysedId,
        requestId = requestId,
        investigationId = referralId,
        prisonerNumber = "A1234BC",
        caseNoteId = caseNoteId,
        promptKey = "old-prompt",
        promptVersion = 1,
        usualBehaviourRelevancy = 4,
        risksAndTriggersRelevancy = 1,
        protectiveFactorsRelevancy = 0,
      ),
    )
    whenever(jdbcTemplate.update(any<String>(), any<MapSqlParameterSource>())).thenThrow(RuntimeException("boom"))

    assertThrows<RuntimeException> {
      service.persistSynchronousAnnotations(response, "A1234BC")
    }

    verify(caseNoteAnnotationRepository).deleteByCaseNotesAnalysedId(existingAnalysedId)
    verify(caseNoteAnalysedRepository).saveAndFlush(any<CaseNoteAnalysed>())
  }

  @Test
  fun `processQueuedCaseNoteAnnotations acknowledges successfully persisted message`() {
    stubCsipRecordLookup()

    val response = testResponse()

    whenever(jdaService.getCaseNoteAnnotationsFromQueue())
      .thenReturn(response)
      .thenReturn(null)

    service.processQueuedCaseNoteAnnotations()

    verify(jdaService).acknowledgeCaseNoteAnnotationsMessage(response.receiptId)
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
          annotation(caseNoteId = caseNoteId, annotatedText = "became agitated", relevancy = 1),
          annotation(caseNoteId = caseNoteId, annotatedText = "raised his voice", relevancy = 3),
        ),
      )
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId))
      .thenReturn(caseNote(caseNoteId, text = "Prisoner became agitated and raised his voice."))

    val response = service.buildSuggestedCaseNotes("A1234AA", referralId, suggestedRequest(), testUsername)

    assertThat(response.suggestedCaseNotes).hasSize(1)
    assertThat(response.suggestedCaseNotes.first().relevance).isEqualTo("high")
    assertThat(response.suggestedCaseNotes.first().annotatedCaseNote)
      .contains("<span class=\"annotation-type\">became agitated</span>")
      .contains("<span class=\"annotation-type\">raised his voice</span>")
  }

  @Test
  fun `buildSuggestedCaseNotes only highlights the case note with matching annotations`() {
    val caseNoteId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
    val caseNoteId2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174002")
    val sharedText = "same text in amendment and other case note"

    whenever(caseNoteAnalysedRepository.findByPrisonerNumberAndInvestigationId("A1234AA", referralId))
      .thenReturn(
        listOf(
          CaseNoteAnalysed(
            requestId = UUID.randomUUID(),
            investigationId = referralId,
            prisonerNumber = "A1234AA",
            caseNoteId = caseNoteId1,
            promptKey = "case-note-analysis",
            promptVersion = 3,
            usualBehaviourRelevancy = 0,
            risksAndTriggersRelevancy = 3,
            protectiveFactorsRelevancy = 0,
          ),
          CaseNoteAnalysed(
            requestId = UUID.randomUUID(),
            investigationId = referralId,
            prisonerNumber = "A1234AA",
            caseNoteId = caseNoteId2,
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
          annotation(caseNoteId = caseNoteId2, annotatedText = sharedText),
        ),
      )
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId1))
      .thenReturn(caseNote(caseNoteId1, text = sharedText))
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId2))
      .thenReturn(
        caseNote(
          caseNoteId2,
          text = "case note body that does not match",
          amendments = listOf(amendment(sharedText)),
        ),
      )

    val response = service.buildSuggestedCaseNotes("A1234AA", referralId, suggestedRequest(), testUsername)
    val note1 = response.suggestedCaseNotes.first { it.caseNoteId == caseNoteId1 }
    val note2 = response.suggestedCaseNotes.first { it.caseNoteId == caseNoteId2 }

    assertThat(note1.annotatedCaseNote).isEqualTo(sharedText)
    assertThat(note1.annotatedCaseNote).doesNotContain("<span class=\"annotation-type\">")
    assertThat(note1.amendments).isEmpty()
    assertThat(note2.annotatedCaseNote).doesNotContain("<span class=\"annotation-type\">")
    assertThat(note2.amendments).hasSize(1)
    assertThat(note2.amendments.first().annotatedText)
      .contains("<span class=\"annotation-type\">$sharedText</span>")
  }

  @Test
  fun `buildSuggestedCaseNotes excludes sensitive case notes when user lacks required roles`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174005")

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
      .thenReturn(emptyList())
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId))
      .thenReturn(caseNote(caseNoteId, text = "Sensitive case note", sensitive = true))
    whenever(userService.getUserRoles("TEST_USER")).thenReturn(emptyList())

    val response = service.buildSuggestedCaseNotes("A1234AA", referralId, suggestedRequest(), testUsername)

    assertThat(response.suggestedCaseNotes).isEmpty()
  }

  @Test
  fun `buildSuggestedCaseNotes keeps non-sensitive case notes but excludes sensitive ones when user lacks required roles`() {
    val sensitiveCaseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174007")
    val normalCaseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174008")

    whenever(caseNoteAnalysedRepository.findByPrisonerNumberAndInvestigationId("A1234AA", referralId))
      .thenReturn(
        listOf(
          CaseNoteAnalysed(
            requestId = UUID.randomUUID(),
            investigationId = referralId,
            prisonerNumber = "A1234AA",
            caseNoteId = sensitiveCaseNoteId,
            promptKey = "case-note-analysis",
            promptVersion = 3,
            usualBehaviourRelevancy = 0,
            risksAndTriggersRelevancy = 3,
            protectiveFactorsRelevancy = 0,
          ),
          CaseNoteAnalysed(
            requestId = UUID.randomUUID(),
            investigationId = referralId,
            prisonerNumber = "A1234AA",
            caseNoteId = normalCaseNoteId,
            promptKey = "case-note-analysis",
            promptVersion = 3,
            usualBehaviourRelevancy = 0,
            risksAndTriggersRelevancy = 2,
            protectiveFactorsRelevancy = 0,
          ),
        ),
      )
    whenever(caseNoteAnnotationRepository.findByCaseNotesAnalysedIdInAndBehaviourType(any(), eq(BehaviourType.RISKS_AND_TRIGGERS)))
      .thenReturn(emptyList())
    whenever(caseNotesClient.getCaseNote("A1234AA", sensitiveCaseNoteId))
      .thenReturn(caseNote(sensitiveCaseNoteId, text = "Sensitive case note", sensitive = true))
    whenever(caseNotesClient.getCaseNote("A1234AA", normalCaseNoteId))
      .thenReturn(caseNote(normalCaseNoteId, text = "Normal case note", sensitive = false))
    whenever(userService.getUserRoles("TEST_USER")).thenReturn(emptyList())

    val response = service.buildSuggestedCaseNotes("A1234AA", referralId, suggestedRequest(), testUsername)

    assertThat(response.suggestedCaseNotes).hasSize(1)
    assertThat(response.suggestedCaseNotes.first().caseNoteId).isEqualTo(normalCaseNoteId)
  }

  @Test
  fun `buildSuggestedCaseNotes includes sensitive case notes when user has required roles`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174006")

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
      .thenReturn(emptyList())
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId))
      .thenReturn(caseNote(caseNoteId, text = "Sensitive case note", sensitive = true))
    whenever(userService.getUserRoles("TEST_USER")).thenReturn(listOf("POM"))

    val response = service.buildSuggestedCaseNotes("A1234AA", referralId, suggestedRequest(), testUsername)

    assertThat(response.suggestedCaseNotes).hasSize(1)
  }

  @Test
  fun `buildSuggestedCaseNotes excludes case notes with relevancy one`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174003")

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
            risksAndTriggersRelevancy = 1,
            protectiveFactorsRelevancy = 0,
          ),
        ),
      )
    whenever(caseNoteAnnotationRepository.findByCaseNotesAnalysedIdInAndBehaviourType(any(), eq(BehaviourType.RISKS_AND_TRIGGERS)))
      .thenReturn(emptyList())

    val response = service.buildSuggestedCaseNotes("A1234AA", referralId, suggestedRequest(), testUsername)

    assertThat(response.suggestedCaseNotes).isEmpty()
  }

  @Test
  fun `buildSuggestedCaseNotes returns case notes with relevancy two even when there are no annotations`() {
    val caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174004")

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
            risksAndTriggersRelevancy = 2,
            protectiveFactorsRelevancy = 0,
          ),
        ),
      )
    whenever(caseNoteAnnotationRepository.findByCaseNotesAnalysedIdInAndBehaviourType(any(), eq(BehaviourType.RISKS_AND_TRIGGERS)))
      .thenReturn(emptyList())
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId))
      .thenReturn(caseNote(caseNoteId, text = "Case note text"))

    val response = service.buildSuggestedCaseNotes("A1234AA", referralId, suggestedRequest(), testUsername)

    assertThat(response.suggestedCaseNotes).hasSize(1)
    assertThat(response.suggestedCaseNotes.first().caseNoteId).isEqualTo(caseNoteId)
    assertThat(response.suggestedCaseNotes.first().annotatedCaseNote).isEqualTo("Case note text")
    assertThat(response.suggestedCaseNotes.first().amendments).isEmpty()
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
      service.buildSuggestedCaseNotes("NOT_FOUND", referralId, suggestedRequest(), testUsername)
    }

    assertThat(exception.message).isEqualTo("Prisoner number invalid")
    verify(personSummaryService).validatePrisoner("NOT_FOUND")
  }

  private fun stubCsipRecordLookup(prisonNumber: String = "A1234BC") {
    val csipRecord = mock<CsipRecord>()
    whenever(csipRecord.prisonNumber).thenReturn(prisonNumber)
    whenever(csipRecordService.retrieveCsipRecord(any())).thenReturn(csipRecord)
  }

  private fun amendment(
    additionalNoteText: String,
    creationDateTime: LocalDateTime = LocalDateTime.now(),
  ) = CaseNoteAmendment(
    creationDateTime = creationDateTime,
    authorUserName = "testuser",
    authorName = "Test User",
    authorUserId = "USER1",
    additionalNoteText = additionalNoteText,
    id = UUID.randomUUID(),
  )

  private fun annotation(
    caseNoteId: UUID,
    annotatedText: String,
    behaviourType: BehaviourType = BehaviourType.RISKS_AND_TRIGGERS,
    relevancy: Int = 3,
  ): CaseNoteAnnotation {
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
    sensitive: Boolean = false,
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
    sensitive = sensitive,
    amendments = amendments,
  )

  private fun suggestedRequest() = SuggestedCaseNotesRequest(
    referralId = referralId,
    behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
    sortField = "relevance",
    sortOrder = "desc",
  )

  private fun testResponse(
    receiptId: String = "receipt-${UUID.randomUUID()}",
    responseData: List<JdaDequeueResponseData>? = listOf(
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
  ) = JdaDequeueResponse(
    requestId = UUID.fromString("f091bc73-4f88-4ff6-9e50-5148d29ed3f6"),
    correlationId = UUID.fromString("f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb"),
    receiptId = receiptId,
    prompt = JdaPrompt(
      key = "case-note-analysis",
      version = 3,
    ),
    status = JdaDequeueResponseStatus.SUCCEEDED,
    responseData = responseData,
    metaData = JdaDequeueResponseMetadata(
      requestType = JdaRequestType.ASYNC,
      completedAt = LocalDateTime.now(),
      completionMs = 1200,
    ),
  )

  private fun testJdaRequestResponse(
    requestId: UUID = UUID.randomUUID(),
    correlationId: UUID = UUID.randomUUID(),
    responseData: List<JdaDequeueResponseData> = listOf(
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
  ) = JdaRequestResponse(
    requestId = requestId,
    correlationId = correlationId,
    prompt = JdaPrompt(
      key = "case-note-analysis",
      version = 3,
    ),
    status = JdaRequestStatus.SUCCEEDED,
    responseData = responseData,
    metaData = JdaMetadata(
      requestType = JdaRequestType.SYNC,
      submittedAt = java.time.OffsetDateTime.now(),
    ),
  )
}
