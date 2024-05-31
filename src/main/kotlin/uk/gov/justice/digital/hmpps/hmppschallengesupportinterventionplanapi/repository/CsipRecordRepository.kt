package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import java.util.UUID

@Repository
interface CsipRecordRepository : JpaRepository<CsipRecord, Long> {
  fun findByRecordUuid(recordId: UUID): CsipRecord?
}
