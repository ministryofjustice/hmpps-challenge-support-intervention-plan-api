package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toReferenceDataModels
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository

@Service
@Transactional
class ReferenceDataService(
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun getReferenceDataForDomain(domain: ReferenceDataType, includeInactive: Boolean): Collection<ReferenceData> =
    referenceDataRepository.findByDomain(domain).toReferenceDataModels(includeInactive)
}
