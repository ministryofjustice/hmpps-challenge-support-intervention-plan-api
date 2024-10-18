package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummaryRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.summaryHasStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.summaryMatchesName
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.summaryMatchesPrison
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.summaryMatchesPrisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResult
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResults
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.PageMeta
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.FindCsipRequest

@Service
@Transactional(readOnly = true)
class CsipSearchService(private val csipSummaryRepository: CsipSummaryRepository) {
  fun findMatchingCsipRecords(request: FindCsipRequest): CsipSearchResults =
    csipSummaryRepository.findAll(request.asSpecification(), request.pageable())
      .map { it.toSearchResult() }.asCsipSearchResults()
}

private fun Page<CsipSearchResult>.asCsipSearchResults() = CsipSearchResults(
  content,
  PageMeta(totalElements),
)

private fun FindCsipRequest.asSpecification(): Specification<CsipSummary> = listOfNotNull(
  summaryMatchesPrison(prisonCode),
  query?.trim()?.let {
    if (it.isPrisonNumber()) {
      summaryMatchesPrisonNumber(it)
    } else {
      summaryMatchesName(it)
    }
  },
  status?.let { summaryHasStatus(it) },
).reduce { spec, current -> spec.and(current) }

private fun CsipSummary.toSearchResult() =
  CsipSearchResult(id, prisoner(), referralDate, nextReviewDate, caseManager, status)

private fun CsipSummary.prisoner() = Prisoner(prisonNumber, firstName, lastName, cellLocation)

private fun String.isPrisonNumber() = matches(Regex("\\w\\d{4}\\w{2}"))
