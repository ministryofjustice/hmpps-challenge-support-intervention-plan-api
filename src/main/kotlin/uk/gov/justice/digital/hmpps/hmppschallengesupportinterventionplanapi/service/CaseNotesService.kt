package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipAssistConfig
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.INVESTIGATION_REQUIRED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import java.time.Clock
import java.time.LocalDateTime

@Service
class CaseNotesService(
  private val caseNotesClient: CaseNotesClient,
  private val csipAssistConfig: CsipAssistConfig,
  @Value("\${feature.csip-assist}")
  private val featureFlag: Boolean,
  private val clock: Clock,
) {
  fun getCaseNotes(request: CaseNotesLookupRequest, params: CaseNotesFilterParams = CaseNotesFilterParams()) {
    if (!featureFlag || request.outcomeTypeCode != INVESTIGATION_REQUIRED || !csipAssistConfig.isActivePrison(request.caseload)) return
    val now = LocalDateTime.now(clock)
    caseNotesClient.getCaseNotes(
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
