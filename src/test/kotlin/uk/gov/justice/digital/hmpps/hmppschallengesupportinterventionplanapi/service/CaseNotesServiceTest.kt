package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import java.time.LocalDateTime

class CaseNotesServiceTest {
  private val caseNotesClient = mock<CaseNotesClient>()
  private val csipAssistConfig = mock<CsipAssistConfig>()
  private val service = CaseNotesService(caseNotesClient, csipAssistConfig, true)
  private val serviceWithFeatureFalse = CaseNotesService(caseNotesClient, csipAssistConfig, false)

  private lateinit var offenderIdentifier: String
  private lateinit var request: Request
  private val params = CaseNotesFilterParams()

  @BeforeEach
  fun setUp() {
    offenderIdentifier = "A1234AA"
    request = Request(
      offenderIdentifier = offenderIdentifier,
      outcomeTypeCode = "OPE",
      includeSensitive = false,
      caseload = "NMI",
    )
  }

  @Test
  fun `getCaseNotes calls to case notes client when feature flag is enabled outcome is OPE and prison is active`() {
    whenever(csipAssistConfig.isActivePrison(request.caseload)).thenReturn(true)

    val before = LocalDateTime.now()
    service.getCaseNotes(request, params)
    val after = LocalDateTime.now()
    val caseNotesRequestCaptor = argumentCaptor<CaseNotesRequest>()

    verify(csipAssistConfig).isActivePrison(request.caseload)
    verify(caseNotesClient).getCaseNotes(eq(offenderIdentifier), caseNotesRequestCaptor.capture())

    val sentRequest = caseNotesRequestCaptor.firstValue
    assertThat(sentRequest.includeSensitive).isFalse()
    assertThat(sentRequest.typeSubTypes).isEmpty()
    assertThat(sentRequest.page).isEqualTo(1)
    assertThat(sentRequest.size).isEqualTo(100)
    assertThat(sentRequest.sort).isEqualTo("occurredAt,desc")
    assertThat(sentRequest.occurredTo).isBetween(before, after)
    assertThat(sentRequest.occurredFrom).isBetween(before.minusDays(90), after.minusDays(90))
  }

  @Test
  fun `getCaseNotes does not call case notes client when outcomeTypeCode is not OPE`() {
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
