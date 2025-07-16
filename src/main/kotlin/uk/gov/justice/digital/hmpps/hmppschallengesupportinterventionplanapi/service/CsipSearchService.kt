package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerNumbers
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummaryRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.status
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.summaryHasStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.summaryMatchesName
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.summaryMatchesPrisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.summaryPrisonInvolvement
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.summaryWithoutRestrictedPatients
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipCounts
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipOverview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResult
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResults
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.PageMeta
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.FindCsipRequest

@Service
@Transactional(readOnly = true)
class CsipSearchService(
  private val csipSummaryRepository: CsipSummaryRepository,
) {
  fun findMatchingCsipRecords(request: FindCsipRequest): CsipSearchResults = with(request) {
    require(prisonCode.isNotEmpty() || query?.matches(PrisonerNumbers.regex) == true) { "At least one prison code or a prison number must be provided" }
    csipSummaryRepository.findAll(asSpecification(), pageable()).map { it.toSearchResult() }.asCsipSearchResults()
  }

  fun getOverviewForPrison(prisonCode: String): CsipOverview = CsipOverview(csipSummaryRepository.getOverviewCounts(prisonCode) ?: CsipCounts.NONE)

  private fun FindCsipRequest.asSpecification(): Specification<CsipSummary> = listOfNotNull(
    if (prisonCode.isNotEmpty()) summaryPrisonInvolvement(prisonCode.toSet()) else null,
    queryString()?.let {
      if (it.isPrisonNumber()) {
        summaryMatchesPrisonNumber(it)
      } else {
        summaryMatchesName(it)
      }
    },
    if (status.isNotEmpty()) summaryHasStatus(status.toSet()) else null,
    if (includeRestrictedPatients) null else summaryWithoutRestrictedPatients(),
  ).reduce { spec, current -> spec.and(current) }
}

private fun Page<CsipSearchResult>.asCsipSearchResults() = CsipSearchResults(
  content,
  PageMeta(totalElements),
)

private fun CsipSummary.toSearchResult() = CsipSearchResult(id, logCode, prisoner(), referralDate, incidentType, nextReviewDate, caseManager, status())

private fun CsipSummary.prisoner() = Prisoner(prisonNumber, firstName, lastName, cellLocation)

private fun String.isPrisonNumber() = matches(PrisonerNumbers.regex)
