package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toCsipRecordEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.CsipRecordNotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import java.util.UUID

@Service
@Transactional
class SaferCustodyScreeningOutcomeService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun createScreeningOutcome(
    recordUuid: UUID,
    request: CreateSaferCustodyScreeningOutcomeRequest,
    context: CsipRequestContext,
  ): SaferCustodyScreeningOutcome {
    val outcomeType = getOutcomeType(request.outcomeTypeCode)

    return csipRecordRepository.findByRecordUuid(recordUuid)?.let {
      csipRecordRepository.saveAndFlush(
        request.toCsipRecordEntity(
          csipRecord = it,
          outcomeType = outcomeType,
          recordedAt = context.requestAt,
          recordedBy = context.username,
          recordedByDisplayName = context.userDisplayName,
          source = context.source,
          activeCaseLoadId = context.activeCaseLoadId,
        ),
      ).saferCustodyScreeningOutcome()!!.toModel()
    } ?: throw CsipRecordNotFoundException("Could not find CSIP record with UUID $recordUuid")
  }

  private fun getOutcomeType(code: String) =
    referenceDataRepository.findByDomainAndCode(ReferenceDataType.OUTCOME_TYPE, code)?.also {
      require(it.isActive()) { "OUTCOME_TYPE code '$code' is inactive" }
    } ?: throw IllegalArgumentException("OUTCOME_TYPE code '$code' does not exist")
}
