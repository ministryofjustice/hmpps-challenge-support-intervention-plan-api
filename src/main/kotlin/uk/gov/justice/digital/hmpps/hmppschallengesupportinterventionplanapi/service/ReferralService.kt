package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.verifyAllReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.ContributoryFactorRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.getContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.saveAndRefresh
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PublishCsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.MergeReferralRequest
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
    verify(referral.doesNotHaveFactor(request.factorTypeCode)) {
      ResourceAlreadyExistException(FACTOR_ALREADY_EXISTS)
    }
    return referral.addContributoryFactor(request, referenceDataRepository::getActiveReferenceData).toModel()
  }

  @PublishCsipEvent(CSIP_UPDATED)
  fun updateContributoryFactor(factorId: UUID, request: UpdateContributoryFactorRequest): ContributoryFactor {
    val factor = factorRepository.getContributoryFactor(factorId)
    verify(factor.referral.doesNotHaveFactor(request.factorTypeCode, factor.id)) {
      ResourceAlreadyExistException(FACTOR_ALREADY_EXISTS)
    }
    return factor.update(request, referenceDataRepository::getActiveReferenceData).toModel()
  }

  @PublishCsipEvent(CSIP_UPDATED)
  fun mergeReferral(csipId: UUID, request: MergeReferralRequest): CsipRecord {
    val record = csipRecordRepository.getCsipRecord(csipId)
    val referral = requireNotNull(record.referral) { MissingReferralException(csipId) }
    val rdMap = referenceDataRepository.verifyAllReferenceData(
      ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE,
      request.contributoryFactors.map { it.factorTypeCode }.toSet(),
    )
    referral.update(request) { type, code -> referenceDataRepository.getActiveReferenceData(type, code) }
    request.contributoryFactors.forEach { toMerge ->
      if (toMerge.factorUuid == null) {
        verify(referral.doesNotHaveFactor(toMerge.factorTypeCode)) {
          ResourceAlreadyExistException(FACTOR_ALREADY_EXISTS)
        }
        referral.addContributoryFactor(toMerge) { type, code -> requireNotNull(rdMap[ReferenceDataKey(type, code)]) }
      } else {
        referral.contributoryFactors().single { it.id == toMerge.factorUuid }.update(toMerge) { type, code ->
          requireNotNull(rdMap[ReferenceDataKey(type, code)])
        }
      }
    }
    return csipRecordRepository.saveAndRefresh(record).toModel()
  }

  companion object {
    private const val FACTOR_ALREADY_EXISTS = "Contributory factor already part of referral"
  }
}

private fun Referral.doesNotHaveFactor(typeCode: String, id: UUID? = null) =
  contributoryFactors().none { it.contributoryFactorType.code == typeCode && it.id != id }
