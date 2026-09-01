package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNotesService
import java.util.UUID

class SuggestedCaseNotesControllerTest {

  private val caseNotesService = mock<CaseNotesService>()
  private val enabledController = SuggestedCaseNotesController(caseNotesService, true)
  private val disabledController = SuggestedCaseNotesController(caseNotesService, false)

  private val prisonerNumber = "A1234AA"

  private val request = SuggestedCaseNotesRequest(
    behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
    sortField = "relevance",
    sortOrder = "desc",
  )

  @Test
  fun `feature enabled - suggestedCaseNotes delegates to service and returns response`() {
    val expected = SuggestedCaseNotesResponse(
      prisonerNumber = prisonerNumber,
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      sortField = "relevance",
      sortOrder = "desc",
      suggestedCaseNotes = listOf(
        SuggestedCaseNote(
          relevance = "high",
          caseNoteId = UUID.fromString("f4ee95d0-49a4-46a2-a485-b8f26f089170"),
          annotatedCaseNote = "Prisoner became agitated during morning medication round.",
        ),
      ),
    )

    whenever(caseNotesService.buildSuggestedCaseNotes(prisonerNumber, request)).thenReturn(expected)

    val response = enabledController.suggestedCaseNotes(prisonerNumber, request)

    verify(caseNotesService).validatePrisonerExists(prisonerNumber)
    verify(caseNotesService).buildSuggestedCaseNotes(prisonerNumber, request)
    verifyNoMoreInteractions(caseNotesService)
    assertThat(response).isEqualTo(expected)
  }

  @Test
  fun `feature enabled - invalid prisoner throws and does not call buildSuggestedCaseNotes`() {
    whenever(caseNotesService.validatePrisonerExists(prisonerNumber)).thenThrow(IllegalArgumentException("Prisoner number invalid"))

    val exception = assertThrows<IllegalArgumentException> {
      enabledController.suggestedCaseNotes(prisonerNumber, request)
    }

    assertThat(exception.message).isEqualTo("Prisoner number invalid")
    verify(caseNotesService).validatePrisonerExists(prisonerNumber)
    verify(caseNotesService, never()).buildSuggestedCaseNotes(prisonerNumber, request)
  }

  @Test
  fun `feature disabled - suggestedCaseNotes returns method not allowed and does not call service`() {
    val exception = assertThrows<ResponseStatusException> {
      disabledController.suggestedCaseNotes(prisonerNumber, request)
    }

    assertThat(exception.statusCode).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
    verifyNoInteractions(caseNotesService)
  }

  @Test
  fun `BehaviourType string values match JDA-380 contract`() {
    assertThat(BehaviourType.RISKS_AND_TRIGGERS.value).isEqualTo("risks_and_triggers")
    assertThat(BehaviourType.USUAL_BEHAVIOUR_PRESENTATION.value).isEqualTo("usual_behaviour_presentation")
    assertThat(BehaviourType.PROTECTIVE_FACTORS.value).isEqualTo("protective_factors")
  }

  @Test
  fun `BehaviourType from resolves each string value case-insensitively`() {
    assertThat(BehaviourType.from("risks_and_triggers")).isEqualTo(BehaviourType.RISKS_AND_TRIGGERS)
    assertThat(BehaviourType.from("USUAL_BEHAVIOUR_PRESENTATION")).isEqualTo(BehaviourType.USUAL_BEHAVIOUR_PRESENTATION)
    assertThat(BehaviourType.from("Protective_Factors")).isEqualTo(BehaviourType.PROTECTIVE_FACTORS)
  }

  @Test
  fun `BehaviourType from throws for unknown value`() {
    assertThrows<IllegalArgumentException> {
      BehaviourType.from("unknown")
    }
  }

  @Test
  fun `can construct request with all fields`() {
    val testRequest = SuggestedCaseNotesRequest(
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      sortField = "relevance",
      sortOrder = "desc",
    )

    assertThat(testRequest.behaviourType).isEqualTo(BehaviourType.RISKS_AND_TRIGGERS)
    assertThat(testRequest.sortField).isEqualTo("relevance")
    assertThat(testRequest.sortOrder).isEqualTo("desc")
  }
}
