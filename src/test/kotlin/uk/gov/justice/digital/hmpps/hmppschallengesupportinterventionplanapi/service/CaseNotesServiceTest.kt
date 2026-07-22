package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipAssistConfig
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.INVESTIGATION_REQUIRED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class CaseNotesServiceTest {
  private val caseNotesClient = mock<CaseNotesClient>()
  private val csipAssistConfig = mock<CsipAssistConfig>()
  private val fixedClock = Clock.fixed(Instant.parse("2026-07-21T07:00:00Z"), ZoneOffset.UTC)
  private val service = CaseNotesService(caseNotesClient, csipAssistConfig, true, fixedClock)
  private val serviceWithFeatureFalse = CaseNotesService(caseNotesClient, csipAssistConfig, false, fixedClock)

  private lateinit var offenderIdentifier: String
  private lateinit var request: CaseNotesLookupRequest
  private val params = CaseNotesFilterParams()

  @BeforeEach
  fun setUp() {
    offenderIdentifier = "A1234AA"
    request = CaseNotesLookupRequest(
      offenderIdentifier = offenderIdentifier,
      outcomeTypeCode = INVESTIGATION_REQUIRED,
      includeSensitive = false,
      caseload = "NMI",
    )
  }

  @Test
  @DisplayName("getCaseNotes calls to case notes client when feature flag is enabled outcome is $INVESTIGATION_REQUIRED and prison is active")
  fun `getCaseNotes calls to case notes client when feature flag is enabled and prison is active`() {
    whenever(csipAssistConfig.isActivePrison(request.caseload)).thenReturn(true)

    service.getCaseNotes(request, params)
    val caseNotesRequestCaptor = argumentCaptor<CaseNotesRequest>()

    verify(csipAssistConfig).isActivePrison(request.caseload)
    verify(caseNotesClient).getCaseNotes(eq(offenderIdentifier), caseNotesRequestCaptor.capture())

    val sentRequest = caseNotesRequestCaptor.firstValue
    val expectedNow = LocalDateTime.ofInstant(fixedClock.instant(), ZoneOffset.UTC)
    assertThat(sentRequest.includeSensitive).isFalse()
    assertThat(sentRequest.typeSubTypes).isEmpty()
    assertThat(sentRequest.page).isEqualTo(1)
    assertThat(sentRequest.size).isEqualTo(100)
    assertThat(sentRequest.sort).isEqualTo("occurredAt,desc")
    assertThat(sentRequest.occurredTo).isEqualTo(expectedNow)
    assertThat(sentRequest.occurredFrom).isEqualTo(expectedNow.minusDays(90))
  }

  @Test
  @DisplayName("getCaseNotes does not call case notes client when outcomeTypeCode is not $INVESTIGATION_REQUIRED")
  fun `getCaseNotes does not call case notes client when outcomeTypeCode is not required outcome`() {
    val nonOpeRequest = request.copy(outcomeTypeCode = "ACC")

    service.getCaseNotes(nonOpeRequest, params)

    verify(csipAssistConfig, never()).isActivePrison(any())
    verify(caseNotesClient, never()).getCaseNotes(any(), any())
  }

  @Test
  fun `getCaseNotes does not call case notes client when prison is not active`() {
    whenever(csipAssistConfig.isActivePrison(request.caseload)).thenReturn(false)

    service.getCaseNotes(request, params)

    verify(csipAssistConfig).isActivePrison(request.caseload)
    verify(caseNotesClient, never()).getCaseNotes(any(), any())
  }

  @Test
  fun `getCaseNotes does not call case notes client when feature flag is false`() {
    serviceWithFeatureFalse.getCaseNotes(request, params)

    verify(csipAssistConfig, never()).isActivePrison(any())
    verify(caseNotesClient, never()).getCaseNotes(any(), any())
  }
}
