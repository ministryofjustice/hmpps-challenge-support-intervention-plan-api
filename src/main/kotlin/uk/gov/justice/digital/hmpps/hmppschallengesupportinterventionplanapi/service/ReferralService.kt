package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ContributoryFactorRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import java.util.UUID

@Service
@Transactional
class ReferralService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val factorRepository: ContributoryFactorRepository,
) {
  fun addContributoryFactor(
    recordUuid: UUID,
    request: CreateContributoryFactorRequest,
  ): ContributoryFactor {
    val record = csipRecordRepository.getCsipRecord(recordUuid)
    val referral = requireNotNull(record.referral) { MissingReferralException(recordUuid) }
    verify(referral.contributoryFactors().none { it.contributoryFactorType.code == request.factorTypeCode }) {
      ResourceAlreadyExistException("Contributory factor already part of referral")
    }
    return referral.addContributoryFactor(request, referenceDataRepository::getActiveReferenceData).toModel()
  }

  fun updateContributoryFactor(factorId: UUID, request: UpdateContributoryFactorRequest): ContributoryFactor {
    val factor = factorRepository.getContributoryFactor(factorId)
    verify(
      factor.referral.contributoryFactors()
        .none { it.contributoryFactorType.code == request.factorTypeCode && it.id != factorId },
    ) {
      ResourceAlreadyExistException("Contributory factor already part of referral")
    }
    return factor.update(request, referenceDataRepository::getActiveReferenceData).toModel()
  }
}
