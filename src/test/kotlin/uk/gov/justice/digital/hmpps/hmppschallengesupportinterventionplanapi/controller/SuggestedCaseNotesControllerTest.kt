package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNotesService
import java.util.UUID

class SuggestedCaseNotesControllerTest {

  private val caseNotesService = mock<CaseNotesService>()
  private val controller = SuggestedCaseNotesController(caseNotesService)

  private val prisonerNumber = "A1234AA"

  private val request = SuggestedCaseNotesRequest(
    referralId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
    sortField = "relevance",
    sortOrder = "desc",
  )

  @Test
  fun `suggestedCaseNotes delegates to service and returns response`() {
    val expected = SuggestedCaseNotesResponse(
      prisonerNumber = prisonerNumber,
      referralId = request.referralId,
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

    val response = controller.suggestedCaseNotes(prisonerNumber, request)

    verify(caseNotesService).buildSuggestedCaseNotes(prisonerNumber, request)
    assertThat(response).isEqualTo(expected)
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
    org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
      BehaviourType.from("unknown")
    }
  }

  @Test
  fun `can construct request with all fields`() {
    val testRequest = SuggestedCaseNotesRequest(
      referralId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
      sortField = "relevance",
      sortOrder = "desc",
    )

    assertThat(testRequest.referralId).isEqualTo("3fa85f64-5717-4562-b3fc-2c963f66afa6")
    assertThat(testRequest.behaviourType).isEqualTo(BehaviourType.RISKS_AND_TRIGGERS)
    assertThat(testRequest.sortField).isEqualTo("relevance")
    assertThat(testRequest.sortOrder).isEqualTo("desc")
  }
}
