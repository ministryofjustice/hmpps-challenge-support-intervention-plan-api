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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNoteAmendment
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteAnnotationSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteWithAnnotations
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
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
    val caseNotesResponse = CaseNotesResponse(
      content = listOf(
        caseNote(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), text = "General note").copy(type = "ACCEPTABLE_TYPE"),
        caseNote(UUID.fromString("223e4567-e89b-12d3-a456-426614174000")).copy(type = "ALERT"),
      ),
      hasCaseNotes = true,
      metadata = CaseNotesMetadata(
        totalElements = 2,
        page = 1,
        size = 100,
      ),
    )

    whenever(caseNotesClient.getCaseNotes(eq("A1234AA"), any())).thenReturn(caseNotesResponse)

    val result = service.getCaseNotes(request, params)

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

    assertThat(result.content.map { it.type })
      .containsExactly("ACCEPTABLE_TYPE")
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
    whenever(caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType("A1234AA", BehaviourType.RISKS_AND_TRIGGERS))
      .thenReturn(listOf(annotation(caseNoteId = caseNoteId, annotatedText = "became agitated", confidenceLevel = ConfidenceLevel.HIGH)))
    whenever(caseNotesClient.getCaseNote("A1234AA", caseNoteId))
      .thenReturn(caseNote(caseNoteId, text = "Prisoner became agitated during the session."))

    val response = service.buildSuggestedCaseNotes("A1234AA", suggestedRequest())

    assertThat(response.suggestedCaseNotes).hasSize(1)
    val note = response.suggestedCaseNotes.first()
    assertThat(note.caseNoteId).isEqualTo(caseNoteId)
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
  fun `composeAnnotationCaseNote renders a single annotation`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("became agitated"),
    )

    val result = service.composeAnnotationCaseNote(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner <span class=\"annotation-type\">became agitated</span> and later raised his voice.",
    )
  }

  @Test
  fun `composeAnnotationCaseNote renders multiple annotations`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("became agitated", "raised his voice"),
    )

    val result = service.composeAnnotationCaseNote(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner <span class=\"annotation-type\">became agitated</span> and later <span class=\"annotation-type\">raised his voice</span>.",
    )
  }

  @Test
  fun `composeAnnotationCaseNote returns original text when no annotations`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = emptyList(),
    )

    val result = service.composeAnnotationCaseNote(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(originalText)
  }

  @Test
  fun `composeAnnotationCaseNote ignores null annotation text`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf(null, "raised his voice"),
    )

    val result = service.composeAnnotationCaseNote(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner became agitated and later <span class=\"annotation-type\">raised his voice</span>.",
    )
  }

  @Test
  fun `composeAnnotationCaseNote ignores blank annotation text`() {
    val originalText = "Prisoner became agitated and later raised his voice."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("   ", "raised his voice"),
    )

    val result = service.composeAnnotationCaseNote(caseNoteWithAnnotations)

    assertThat(result).isEqualTo(
      "Prisoner became agitated and later <span class=\"annotation-type\">raised his voice</span>.",
    )
  }

  @Test
  fun `composeAnnotationCaseNote preserves original content outside annotations`() {
    val originalText = "On review, prisoner became agitated, then settled down after staff support."
    val caseNoteWithAnnotations = caseNoteWithAnnotations(
      text = originalText,
      annotationTexts = listOf("became agitated"),
    )

    val result = service.composeAnnotationCaseNote(caseNoteWithAnnotations)

    assertThat(result).startsWith("On review, prisoner ")
    assertThat(result).contains("<span class=\"annotation-type\">became agitated</span>")
    assertThat(result).endsWith(", then settled down after staff support.")
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
