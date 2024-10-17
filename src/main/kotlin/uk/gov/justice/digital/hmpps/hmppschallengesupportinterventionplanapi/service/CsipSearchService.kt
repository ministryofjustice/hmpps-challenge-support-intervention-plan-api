package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.hasStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.matchesPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResult
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResults
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.PageMeta
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.FindCsipRequest

@Transactional
@Service
class CsipSearchService(private val csipRepository: CsipRecordRepository) {
  fun findMatchingCsipRecords(request: FindCsipRequest): CsipSearchResults =
    csipRepository.findAll(request.asSpecification(), request.pageable())
      .map { it.toSearchResult() }.asCsipSearchResults()
}

private fun Page<CsipSearchResult>.asCsipSearchResults() = CsipSearchResults(
  content,
  PageMeta(totalElements),
)

private fun FindCsipRequest.asSpecification(): Specification<CsipRecord> = listOfNotNull(
  matchesPersonSummary(prisonCode, query?.trim()),
  status?.let { hasStatus(it) },
).reduce { spec, current -> spec.and(current) }

private fun CsipRecord.toSearchResult() = CsipSearchResult(
  id,
  personSummary.asPrisoner(),
  requireNotNull(referral).referralDate,
  plan?.nextReviewDate(),
  plan?.caseManager,
  status,
)

private fun PersonSummary.asPrisoner() = Prisoner(prisonNumber, firstName, lastName, cellLocation)
