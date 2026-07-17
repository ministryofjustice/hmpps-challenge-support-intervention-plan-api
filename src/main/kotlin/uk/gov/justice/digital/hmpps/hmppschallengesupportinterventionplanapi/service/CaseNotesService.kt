package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipAssistConfig
import java.time.LocalDateTime

@Service
class CaseNotesService(
  private val caseNotesClient: CaseNotesClient,
  private val csipAssistConfig: CsipAssistConfig,
  @Value("\${feature.csip-assist}")
  private val featureFlag: Boolean,
) {
  fun getCaseNotes(request: Request, params: CaseNotesFilterParams) {
    val caseNotesRequest = CaseNotesRequest(
      includeSensitive = request.includeSensitive,
      typeSubTypes = emptyList(),
      occurredFrom = LocalDateTime.now().minusDays(params.period),
      occurredTo = LocalDateTime.now(),
      page = 1,
      size = params.pageSize,
      sort = "occurredAt,desc",
    )

    if (featureFlag && request.outcomeTypeCode == "OPE" && csipAssistConfig.isActivePrison(request.caseload)) {
      caseNotesClient.getCaseNotes(request.offenderIdentifier, caseNotesRequest)
    }
  }
}

data class Request(
  val offenderIdentifier: String,
  val includeSensitive: Boolean = false,
  val outcomeTypeCode: String,
  val caseload: String,
)

data class CaseNotesFilterParams(
  val period: Long = 90,
  val pageSize: Int = 100,
)
