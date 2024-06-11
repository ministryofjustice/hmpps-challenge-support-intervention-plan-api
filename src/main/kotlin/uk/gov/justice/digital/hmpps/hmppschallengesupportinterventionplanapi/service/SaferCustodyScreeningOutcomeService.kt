package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toCsipRecordEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.CsipRecordNotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
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
    val outcomeType = referenceDataRepository.getOutcomeType(request.outcomeTypeCode)

    return csipRecordRepository.findByRecordUuid(recordUuid)?.let {
      it.referral?.let { referral ->
        csipRecordRepository.saveAndFlush(
          request.toCsipRecordEntity(
            referral = referral,
            outcomeType = outcomeType,
            actionedAt = context.requestAt,
            actionedBy = context.username,
            actionedByDisplayName = context.userDisplayName,
            source = context.source,
            activeCaseLoadId = context.activeCaseLoadId,
          ),
        ).referral()!!.saferCustodyScreeningOutcome()!!.toModel()
      } ?: throw MissingReferralException(recordUuid)
    } ?: throw CsipRecordNotFoundException("Could not find CSIP record with UUID $recordUuid")
  }

  private fun ReferenceDataRepository.getOutcomeType(code: String) =
    findByDomainAndCode(ReferenceDataType.OUTCOME_TYPE, code)?.also {
      require(it.isActive()) { "OUTCOME_TYPE code '$code' is inactive" }
    } ?: throw IllegalArgumentException("OUTCOME_TYPE code '$code' does not exist")
}
