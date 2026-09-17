package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
class CaseNotesServiceTest {
  private val caseNotesClient = mock<CaseNotesClient>()
  private val service = CaseNotesService(caseNotesClient)
  private val request = CaseNotesLookupRequest(
    offenderIdentifier = "A1234AA",
    includeSensitive = true,
  )
  private val params = CaseNotesFilterParams()

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
    val before = LocalDateTime.now()
    val result = service.getCaseNotes(request, params)
    val after = LocalDateTime.now()
    val requestCaptor = argumentCaptor<CaseNotesRequest>()
    verify(caseNotesClient)
      .getCaseNotes(
        eq("A1234AA"),
        requestCaptor.capture(),
      )
    val sentRequest = requestCaptor.firstValue
    val expectedFrom = LocalDate.now()
      .minusDays(params.period)
      .atStartOfDay()
    assertThat(sentRequest.includeSensitive).isTrue()
    assertThat(sentRequest.typeSubTypes).isEmpty()
    assertThat(sentRequest.page).isEqualTo(1)
    assertThat(sentRequest.size).isEqualTo(100)
    assertThat(sentRequest.sort).isEqualTo("occurredAt,desc")
    assertThat(sentRequest.occurredTo).isBetween(before, after)
    assertThat(sentRequest.occurredFrom).isEqualTo(expectedFrom)
    assertThat(result.content.map { it.type }).containsExactly("ACCEPTABLE_TYPE")
  }
  private fun caseNote(
    caseNoteId: UUID,
    text: String = "Case note text",
    creationDateTime: LocalDateTime = LocalDateTime.now(),
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
    amendments = emptyList(),
  )
}
