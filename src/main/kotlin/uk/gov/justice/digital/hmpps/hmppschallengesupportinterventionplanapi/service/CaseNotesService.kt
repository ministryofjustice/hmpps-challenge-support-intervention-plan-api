package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import java.time.Clock
import java.time.LocalDateTime

@Service
class CaseNotesService(
  private val caseNotesClient: CaseNotesClient,
  private val clock: Clock,
) {
  fun getCaseNotes(
    request: CaseNotesLookupRequest,
    params: CaseNotesFilterParams = CaseNotesFilterParams(),
  ): CaseNotesResponse {
    val now = LocalDateTime.now(clock)

    return caseNotesClient.getCaseNotes(
      request.offenderIdentifier,
      CaseNotesRequest(
        includeSensitive = request.includeSensitive,
        typeSubTypes = emptyList(),
        occurredFrom = now.minusDays(params.period),
        occurredTo = now,
        page = 1,
        size = params.pageSize,
        sort = "occurredAt,desc",
      ),
    )
  }
}
