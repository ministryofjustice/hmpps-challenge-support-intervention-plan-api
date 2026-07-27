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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
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
        includeSensitive = false,
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
      .isFalse()

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
}
