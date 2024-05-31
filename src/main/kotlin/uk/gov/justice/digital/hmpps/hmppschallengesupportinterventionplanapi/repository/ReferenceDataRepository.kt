package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType

@Repository
interface ReferenceDataRepository : JpaRepository<ReferenceData, Long> {
  fun findByDomain(domain: ReferenceDataType): Collection<ReferenceData>

  fun findByDomainAndCode(domain: ReferenceDataType, code: String): ReferenceData?
}
