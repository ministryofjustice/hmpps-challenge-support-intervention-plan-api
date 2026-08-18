package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_PRISONER_CASE_NOTES_RO
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.CaseNotesServer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
import java.time.LocalDateTime
import java.util.UUID

class SuggestedCaseNotesControllerIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var caseNoteAnnotationRepository: CaseNoteAnnotationRepository

  @BeforeEach
  fun setUp() {
    caseNotesServer.resetAll()
    caseNoteAnnotationRepository.deleteAll()
  }

  @Test
  fun `filters by prisoner and behaviour`() {
    val prisonerNumber = givenValidPrisonNumber("A1111AA")
    val matchingCaseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val ignoredCaseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111112")

    saveAnnotation(
      prisonerNumber = prisonerNumber,
      caseNoteId = matchingCaseNoteId,
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      confidenceLevel = ConfidenceLevel.HIGH,
      annotatedText = "became agitated",
    )
    saveAnnotation(
      prisonerNumber = "A2222AA",
      caseNoteId = ignoredCaseNoteId,
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      confidenceLevel = ConfidenceLevel.HIGH,
      annotatedText = "ignored",
    )

    caseNotesServer.stubGetCaseNoteById(
      offenderIdentifier = prisonerNumber,
      caseNoteId = matchingCaseNoteId,
      occurrenceDateTime = "2026-07-09T11:30:00",
      text = "Prisoner became agitated on the wing.",
    )

    val response = webTestClient.postSuggestedCaseNotes(
      prisonerNumber = prisonerNumber,
      request = suggestedCaseNotesRequest(behaviourType = BehaviourType.RISKS_AND_TRIGGERS),
    ).successResponse<SuggestedCaseNotesResponse>()

    assertThat(response.suggestedCaseNotes).hasSize(1)
    assertThat(response.suggestedCaseNotes.first().caseNoteId).isEqualTo(matchingCaseNoteId)
  }

  @Test
  fun `ignores other behaviour types`() {
    val prisonerNumber = givenValidPrisonNumber("A3333AA")
    val risksCaseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111113")
    val protectiveCaseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111114")

    saveAnnotation(
      prisonerNumber = prisonerNumber,
      caseNoteId = risksCaseNoteId,
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      confidenceLevel = ConfidenceLevel.MEDIUM,
      annotatedText = "raised his voice",
    )
    saveAnnotation(
      prisonerNumber = prisonerNumber,
      caseNoteId = protectiveCaseNoteId,
      behaviourType = BehaviourType.PROTECTIVE_FACTORS,
      confidenceLevel = ConfidenceLevel.HIGH,
      annotatedText = "settled down",
    )

    caseNotesServer.stubGetCaseNoteById(
      offenderIdentifier = prisonerNumber,
      caseNoteId = risksCaseNoteId,
      occurrenceDateTime = "2026-07-09T12:30:00",
      text = "He raised his voice then calmed down.",
    )

    val response = webTestClient.postSuggestedCaseNotes(
      prisonerNumber = prisonerNumber,
      request = suggestedCaseNotesRequest(behaviourType = BehaviourType.RISKS_AND_TRIGGERS),
    ).successResponse<SuggestedCaseNotesResponse>()

    assertThat(response.suggestedCaseNotes).hasSize(1)
    assertThat(response.suggestedCaseNotes.first().caseNoteId).isEqualTo(risksCaseNoteId)
  }

  @Test
  fun `retrieves associated case note content`() {
    val prisonerNumber = givenValidPrisonNumber("A4444AA")
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111115")

    saveAnnotation(
      prisonerNumber = prisonerNumber,
      caseNoteId = caseNoteId,
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      confidenceLevel = ConfidenceLevel.HIGH,
      annotatedText = "became agitated",
    )

    caseNotesServer.stubGetCaseNoteById(
      offenderIdentifier = prisonerNumber,
      caseNoteId = caseNoteId,
      occurrenceDateTime = "2026-07-09T13:30:00",
      text = "During association prisoner became agitated but accepted support.",
    )

    val response = webTestClient.postSuggestedCaseNotes(
      prisonerNumber = prisonerNumber,
      request = suggestedCaseNotesRequest(),
    ).successResponse<SuggestedCaseNotesResponse>()

    assertThat(response.suggestedCaseNotes).hasSize(1)
    assertThat(response.suggestedCaseNotes.first().annotatedCaseNote)
      .contains("During association prisoner")
      .contains("<span class=\"annotation-type\">became agitated</span>")

    caseNotesServer.verify(
      exactly(1),
      getRequestedFor(urlEqualTo("/case-notes/$prisonerNumber/$caseNoteId")),
    )
  }

  @Test
  fun `renders multiple annotations for one case note`() {
    val prisonerNumber = givenValidPrisonNumber("A5555AA")
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111116")

    saveAnnotation(
      prisonerNumber = prisonerNumber,
      caseNoteId = caseNoteId,
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      confidenceLevel = ConfidenceLevel.MEDIUM,
      annotatedText = "became agitated",
    )
    saveAnnotation(
      prisonerNumber = prisonerNumber,
      caseNoteId = caseNoteId,
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      confidenceLevel = ConfidenceLevel.MEDIUM,
      annotatedText = "raised his voice",
    )

    caseNotesServer.stubGetCaseNoteById(
      offenderIdentifier = prisonerNumber,
      caseNoteId = caseNoteId,
      occurrenceDateTime = "2026-07-09T14:30:00",
      text = "Prisoner became agitated and later raised his voice.",
    )

    val response = webTestClient.postSuggestedCaseNotes(
      prisonerNumber = prisonerNumber,
      request = suggestedCaseNotesRequest(),
    ).successResponse<SuggestedCaseNotesResponse>()

    assertThat(response.suggestedCaseNotes).hasSize(1)
    assertThat(response.suggestedCaseNotes.first().annotatedCaseNote)
      .contains("<span class=\"annotation-type\">became agitated</span>")
      .contains("<span class=\"annotation-type\">raised his voice</span>")
  }

  @Test
  fun `response includes required fields`() {
    val prisonerNumber = givenValidPrisonNumber("A6666AA")
    val referralId = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    val caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111117")

    saveAnnotation(
      prisonerNumber = prisonerNumber,
      caseNoteId = caseNoteId,
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      confidenceLevel = ConfidenceLevel.HIGH,
      annotatedText = "became agitated",
    )

    caseNotesServer.stubGetCaseNoteById(
      offenderIdentifier = prisonerNumber,
      caseNoteId = caseNoteId,
      occurrenceDateTime = "2026-07-09T15:30:00",
      text = "Prisoner became agitated before evening unlock.",
    )

    webTestClient.postSuggestedCaseNotes(
      prisonerNumber = prisonerNumber,
      request = suggestedCaseNotesRequest(referralId = referralId),
    )
      .expectBody()
      .jsonPath("$.prisonerNumber").isEqualTo(prisonerNumber)
      .jsonPath("$.referralId").isEqualTo(referralId)
      .jsonPath("$.behaviourType").isEqualTo("risks_and_triggers")
      .jsonPath("$.suggestedCaseNotes[0].case_note_id").isEqualTo(caseNoteId.toString())
      .jsonPath("$.suggestedCaseNotes[0].relevance").isEqualTo("high")
      .jsonPath("$.suggestedCaseNotes[0].annotated_case_note").value<String> {
        assertThat(it).contains("<span class=\"annotation-type\">became agitated</span>")
      }
  }

  @Test
  fun `returns empty list when no annotations match`() {
    val prisonerNumber = givenValidPrisonNumber("A7777AA")

    val response = webTestClient.postSuggestedCaseNotes(
      prisonerNumber = prisonerNumber,
      request = suggestedCaseNotesRequest(),
    ).successResponse<SuggestedCaseNotesResponse>()

    assertThat(response.suggestedCaseNotes).isEmpty()

    caseNotesServer.verify(
      exactly(0),
      getRequestedFor(urlPathMatching("/case-notes/.+")),
    )
  }

  @Test
  fun `orders suggestions by occurrence desc`() {
    val prisonerNumber = givenValidPrisonNumber("A8888AA")
    val olderCaseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111118")
    val newerCaseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111119")

    saveAnnotation(
      prisonerNumber = prisonerNumber,
      caseNoteId = olderCaseNoteId,
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      confidenceLevel = ConfidenceLevel.LOW,
      annotatedText = "stood by cell door",
    )
    saveAnnotation(
      prisonerNumber = prisonerNumber,
      caseNoteId = newerCaseNoteId,
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      confidenceLevel = ConfidenceLevel.MEDIUM,
      annotatedText = "accepted support",
    )

    caseNotesServer.stubGetCaseNoteById(
      offenderIdentifier = prisonerNumber,
      caseNoteId = olderCaseNoteId,
      occurrenceDateTime = "2026-07-09T10:00:00",
      text = "The prisoner stood by cell door during free flow.",
    )
    caseNotesServer.stubGetCaseNoteById(
      offenderIdentifier = prisonerNumber,
      caseNoteId = newerCaseNoteId,
      occurrenceDateTime = "2026-07-10T10:00:00",
      text = "The prisoner accepted support from staff.",
    )

    val response = webTestClient.postSuggestedCaseNotes(
      prisonerNumber = prisonerNumber,
      request = suggestedCaseNotesRequest(),
    ).successResponse<SuggestedCaseNotesResponse>()

    assertThat(response.suggestedCaseNotes).hasSize(2)
    assertThat(response.suggestedCaseNotes[0].caseNoteId).isEqualTo(newerCaseNoteId)
    assertThat(response.suggestedCaseNotes[1].caseNoteId).isEqualTo(olderCaseNoteId)
  }

  @Test
  fun `returns bad request for invalid prisoner`() {
    val response = webTestClient.postSuggestedCaseNotes(
      prisonerNumber = PRISON_NUMBER_NOT_FOUND,
      request = suggestedCaseNotesRequest(),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    assertThat(response.userMessage).isEqualTo("Validation failure: Prisoner number invalid")
    assertThat(response.developerMessage).isEqualTo("Prisoner number invalid")
  }

  private fun WebTestClient.postSuggestedCaseNotes(prisonerNumber: String, request: SuggestedCaseNotesRequest) = post()
    .uri(urlToTest(prisonerNumber))
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_CASE_NOTES_RO)))
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(request)
    .exchange()
    .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun saveAnnotation(
    prisonerNumber: String,
    caseNoteId: UUID,
    behaviourType: BehaviourType,
    confidenceLevel: ConfidenceLevel,
    annotatedText: String,
  ) {
    caseNoteAnnotationRepository.save(
      CaseNoteAnnotation(
        requestId = UUID.randomUUID(),
        prisonerNumber = prisonerNumber,
        caseNoteId = caseNoteId,
        promptKey = "case-note-analysis",
        promptVersion = 1,
        behaviourType = behaviourType,
        confidenceLevel = confidenceLevel,
        annotatedText = annotatedText,
        createdDate = LocalDateTime.now(),
      ),
    )
  }

  private fun suggestedCaseNotesRequest(
    referralId: String = UUID.randomUUID().toString(),
    behaviourType: BehaviourType = BehaviourType.RISKS_AND_TRIGGERS,
  ) = SuggestedCaseNotesRequest(
    referralId = referralId,
    behaviourType = behaviourType,
    sortField = "occurrenceDateTime",
    sortOrder = "desc",
  )

  private fun urlToTest(prisonNumber: String) = "/v1/suggestedCaseNotes/$prisonNumber"

  companion object {
    @JvmField
    internal val caseNotesServer = CaseNotesServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      caseNotesServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      caseNotesServer.stop()
    }
  }
}
