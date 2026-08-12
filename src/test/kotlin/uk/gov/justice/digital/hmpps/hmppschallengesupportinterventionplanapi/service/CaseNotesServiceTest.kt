package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteAnnotationSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteWithAnnotations
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class CaseNotesServiceTest {

  private val caseNotesClient = mock<CaseNotesClient>()
  private val prisonerSearchClient = mock<PrisonerSearchClient>()
  private val caseNoteAnnotationRepository = mock<CaseNoteAnnotationRepository>()

  private val fixedClock =
    Clock.fixed(
      Instant.parse("2026-07-21T07:00:00Z"),
      ZoneOffset.UTC,
    )

  private val service =
    CaseNotesService(
      caseNotesClient,
      prisonerSearchClient,
      fixedClock,
      caseNoteAnnotationRepository,
    )

  private lateinit var request: CaseNotesLookupRequest

  private val params = CaseNotesFilterParams()

  @BeforeEach
  fun setUp() {
    request =
      CaseNotesLookupRequest(
        offenderIdentifier = "A1234AA",
        includeSensitive = true,
      )
  }

  @Test
  fun `getCaseNotes calls case notes client with expected request`() {
    service.getCaseNotes(request, params)

    val requestCaptor =
      argumentCaptor<CaseNotesRequest>()

    verify(caseNotesClient)
      .getCaseNotes(
        eq("A1234AA"),
        requestCaptor.capture(),
      )

    val sentRequest = requestCaptor.firstValue
    val expectedNow =
      LocalDateTime.ofInstant(
        fixedClock.instant(),
        ZoneOffset.UTC,
      )

    assertThat(sentRequest.includeSensitive)
      .isTrue()

    assertThat(sentRequest.typeSubTypes)
      .isEmpty()

    assertThat(sentRequest.page)
      .isEqualTo(1)

    assertThat(sentRequest.size)
      .isEqualTo(100)

    assertThat(sentRequest.sort)
      .isEqualTo("occurredAt,desc")

    assertThat(sentRequest.occurredTo)
      .isEqualTo(expectedNow)

    assertThat(sentRequest.occurredFrom)
      .isEqualTo(expectedNow.minusDays(90))
  }

  @Test
  fun `buildSuggestedCaseNotes returns response with prisoner number and request fields echoed`() {
    whenever(prisonerSearchClient.getPrisoner(any())).thenReturn(prisonerDetails())

    val suggestedRequest = SuggestedCaseNotesRequest(
      referralId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      sortField = "relevance",
      sortOrder = "desc",
    )

    val response = service.buildSuggestedCaseNotes("A1234AA", suggestedRequest)

    assertThat(response.prisonerNumber).isEqualTo("A1234AA")
    assertThat(response.referralId).isEqualTo("3fa85f64-5717-4562-b3fc-2c963f66afa6")
    assertThat(response.behaviourType).isEqualTo(BehaviourType.RISKS_AND_TRIGGERS)
    assertThat(response.sortField).isEqualTo("relevance")
    assertThat(response.sortOrder).isEqualTo("desc")
  }

  @Test
  fun `buildSuggestedCaseNotes returns static suggested case notes for each behaviour type`() {
    whenever(prisonerSearchClient.getPrisoner(any())).thenReturn(prisonerDetails())

    BehaviourType.entries.forEach { behaviourType ->
      val suggestedRequest = SuggestedCaseNotesRequest(
        referralId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        behaviourType = behaviourType,
        sortField = "relevance",
        sortOrder = "desc",
      )

      val response = service.buildSuggestedCaseNotes("A1234AA", suggestedRequest)

      assertThat(response.suggestedCaseNotes)
        .`as`("Expected non-empty suggested case notes for behaviourType $behaviourType")
        .isNotEmpty
      response.suggestedCaseNotes.forEach { note ->
        assertThat(note.relevance).isIn("high", "medium", "low")
        assertThat(note.caseNoteId).isNotNull()
        assertThat(note.annotatedCaseNote).isNotBlank()
      }
    }
  }

  @Test
  fun `validatePrisonerExists passes when prisoner exists`() {
    whenever(prisonerSearchClient.getPrisoner("A1234AA")).thenReturn(prisonerDetails())

    service.validatePrisonerExists("A1234AA")

    verify(prisonerSearchClient).getPrisoner("A1234AA")
  }

  @Test
  fun `validatePrisonerExists throws IllegalArgumentException when prisoner does not exist`() {
    whenever(prisonerSearchClient.getPrisoner("NOT_FOUND")).thenReturn(null)

    val exception = assertThrows<IllegalArgumentException> {
      service.validatePrisonerExists("NOT_FOUND")
    }

    assertThat(exception.message).isEqualTo("Prisoner number invalid")
    verify(prisonerSearchClient).getPrisoner("NOT_FOUND")
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
  fun `renderAnnotatedCaseNote renders a single annotation`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("became agitated"),
    )

    val result = service.renderAnnotatedCaseNote(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner <span class=\"annotation-type\">became agitated</span> and later raised his voice.",
    )
  }

  @Test
  fun `renderAnnotatedCaseNote renders multiple annotations`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("became agitated", "raised his voice"),
    )

    val result = service.renderAnnotatedCaseNote(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner <span class=\"annotation-type\">became agitated</span> and later <span class=\"annotation-type\">raised his voice</span>.",
    )
  }

  @Test
  fun `renderAnnotatedCaseNote returns original text when no annotations`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = emptyList(),
    )

    val result = service.renderAnnotatedCaseNote(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(originalText)
  }

  @Test
  fun `renderAnnotatedCaseNote ignores null annotation text`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf(null, "raised his voice"),
    )

    val result = service.renderAnnotatedCaseNote(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner became agitated and later <span class=\"annotation-type\">raised his voice</span>.",
    )
  }

  @Test
  fun `renderAnnotatedCaseNote ignores blank annotation text`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("   ", "raised his voice"),
    )

    val result = service.renderAnnotatedCaseNote(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner became agitated and later <span class=\"annotation-type\">raised his voice</span>.",
    )
  }

  @Test
  fun `renderAnnotatedCaseNote preserves original content outside annotations`() {
    val originalText = "On review, prisoner became agitated, then settled down after staff support."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("became agitated"),
    )

    val result = service.renderAnnotatedCaseNote(caseNoteWithAnnotations)

    assertThat(result).startsWith("On review, prisoner ")
    assertThat(result).contains("<span class=\"annotation-type\">became agitated</span>")
    assertThat(result).endsWith(", then settled down after staff support.")
  }

  private fun annotation(
    caseNoteId: UUID?,
    annotatedText: String,
    behaviourType: BehaviourType = BehaviourType.RISKS_AND_TRIGGERS,
  ) = CaseNoteAnnotation(
    id = UUID.randomUUID(),
    requestId = UUID.randomUUID(),
    prisonerNumber = "A1234AA",
    caseNoteId = caseNoteId,
    promptKey = "case-note-analysis",
    promptVersion = 3,
    behaviourType = behaviourType,
    confidenceLevel = ConfidenceLevel.HIGH,
    annotatedText = annotatedText,
    createdDate = LocalDateTime.now(),
  )

  private fun caseNote(caseNoteId: UUID, text: String = "Case note text") = CaseNote(
    caseNoteId = caseNoteId,
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
    text = text,
    locationId = "MDI",
    sensitive = false,
    amendments = emptyList(),
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

  private fun prisonerDetails() = PrisonerDetails(
    prisonerNumber = "A1234AA",
    firstName = "First",
    lastName = "Last",
    prisonId = "MDI",
    status = "ACTIVE IN",
    restrictedPatient = false,
    cellLocation = "A-1-001",
    supportingPrisonId = null,
  )
}
