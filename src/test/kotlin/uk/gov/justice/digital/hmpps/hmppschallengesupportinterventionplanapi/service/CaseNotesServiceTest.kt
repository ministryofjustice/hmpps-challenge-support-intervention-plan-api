package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class CaseNotesServiceTest {

  private val caseNotesClient = mock<CaseNotesClient>()

  private val fixedClock =
    Clock.fixed(
      Instant.parse("2026-07-21T07:00:00Z"),
      ZoneOffset.UTC,
    )

  private val service =
    CaseNotesService(
      caseNotesClient,
      fixedClock,
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
}
