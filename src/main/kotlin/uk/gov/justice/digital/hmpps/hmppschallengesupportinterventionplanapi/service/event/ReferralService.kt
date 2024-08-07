package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.event

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import java.util.UUID

@Service
@Transactional
class ReferralService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun addContributoryFactor(
    recordUuid: UUID,
    request: CreateContributoryFactorRequest,
  ): ContributoryFactor {
    val record = csipRecordRepository.getCsipRecord(recordUuid)
    val referral = requireNotNull(record.referral) { MissingReferralException(recordUuid) }
    val factorType =
      referenceDataRepository.getActiveReferenceData(ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE, request.factorTypeCode)
    verify(referral.contributoryFactors().none { it.contributoryFactorType.code == request.factorTypeCode }) {
      ResourceAlreadyExistException("Contributory factor already part of referral")
    }
    val factor = referral.addContributoryFactor(request, factorType, csipRequestContext())
    csipRecordRepository.save(record)
    return factor.toModel()
  }
}
