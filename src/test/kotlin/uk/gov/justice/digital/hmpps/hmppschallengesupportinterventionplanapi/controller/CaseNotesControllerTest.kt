package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.INVESTIGATION_REQUIRED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNotesService

class CaseNotesControllerTest {
  private val caseNotesService = mock<CaseNotesService>()
  private val controller = CaseNotesController(caseNotesService)

  @Test
  fun `returns 202 and no response body`() {
    val request = testRequest()
    val params = CaseNotesFilterParams()

    val result = controller.getCaseNotes(params, request)

    assertThat(result.statusCode).isEqualTo(HttpStatus.ACCEPTED)
    assertThat(result.body).isNull()
    verify(caseNotesService).getCaseNotes(request, params)
  }

  private fun testRequest() = CaseNotesLookupRequest(
    offenderIdentifier = "A1234AA",
    includeSensitive = false,
    outcomeTypeCode = INVESTIGATION_REQUIRED,
    caseload = "LEI",
  )
}
