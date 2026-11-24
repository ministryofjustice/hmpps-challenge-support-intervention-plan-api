package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import java.time.LocalDateTime

@Service
class CaseNotesService(
  private val caseNotesClient: CaseNotesClient,
) {
  fun getCaseNotes(
    request: CaseNotesLookupRequest,
    params: CaseNotesFilterParams = CaseNotesFilterParams(),
  ): CaseNotesResponse {
    val now = LocalDateTime.now()
    val occurredFrom = now
      .toLocalDate()
      .minusDays(params.period)
      .atStartOfDay()

    val caseNotes = caseNotesClient.getCaseNotes(
      request.offenderIdentifier,
      CaseNotesRequest(
        includeSensitive = request.includeSensitive,
        typeSubTypes = emptyList(),
        occurredFrom = occurredFrom,
        occurredTo = now,
        page = 1,
        size = params.pageSize,
        sort = "occurredAt,desc",
      ),
    )
    return caseNotes.copy(content = caseNotes.content.filter { it.type != "ALERT" })
  }

  fun getCaseNote(prisonerNumber: String, caseNoteId: java.util.UUID): CaseNote? = try {
    caseNotesClient.getCaseNote(prisonerNumber, caseNoteId)
  } catch (e: Exception) {
    log.warn(
      "Skipping case note because it could not be retrieved from Case Notes API: prisonerNumber={}, caseNoteId={}",
      prisonerNumber,
      caseNoteId,
      e,
    )
    null
  }

  private companion object {
    private val log = LoggerFactory.getLogger(CaseNotesService::class.java)
  }
}
