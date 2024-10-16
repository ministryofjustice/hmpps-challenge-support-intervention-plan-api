package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData as ReferenceDataEntity

@Service
@Transactional
class ReferenceDataService(
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun getReferenceDataForDomain(domain: ReferenceDataType, includeInactive: Boolean): Collection<ReferenceData> =
    referenceDataRepository.findByKeyDomain(domain).toReferenceDataModels(includeInactive)
}

fun Collection<ReferenceDataEntity>.toReferenceDataModels(includeInactive: Boolean) =
  filter { includeInactive || it.isActive() }.sortedWith(compareBy(ReferenceDataEntity::listSequence))
    .map { it.toReferenceDataModel() }
