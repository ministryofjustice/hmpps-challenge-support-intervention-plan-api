package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.ContributoryFactorRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.getContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PublishCsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpdateContributoryFactorRequest
import java.util.UUID

@Service
@Transactional
class ReferralService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val factorRepository: ContributoryFactorRepository,
) {
  @PublishCsipEvent(CSIP_UPDATED)
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

  @PublishCsipEvent(CSIP_UPDATED)
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
