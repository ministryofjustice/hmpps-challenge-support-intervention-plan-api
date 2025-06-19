package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord.Companion.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import java.time.ZonedDateTime
import java.util.UUID

@Repository
interface CsipRecordRepository :
  JpaRepository<CsipRecord, Long>,
  JpaSpecificationExecutor<CsipRecord>,
  RevisionRepository<CsipRecord, Long, Long>,
  RefreshRepository<CsipRecord, Long> {
  fun findById(recordId: UUID): CsipRecord?

  fun findByLegacyId(legacyId: Long): CsipRecord?

  fun countByPrisonNumber(prisonNumber: String): Int

  fun findByPrisonNumberAndIdIn(prisonNumber: String, ids: Set<UUID>): List<CsipRecord>

  @Query(
    """
      select csip.* from csip_record csip
      join csip_record_audit audit on audit.record_id = csip.record_id
      join audit_revision ar on ar.id = audit.rev_id
      where csip.prison_number = :prisonNumber and ar.timestamp between :from and :to
    """,
    nativeQuery = true,
  )
  fun findByPrisonNumberAndReferralReferralDateBetween(prisonNumber: String, from: ZonedDateTime, to: ZonedDateTime): List<CsipRecord>
}

fun CsipRecordRepository.getCsipRecord(recordUuid: UUID) = findById(recordUuid) ?: throw NotFoundException("CSIP Record", recordUuid.toString())

fun CsipRecordRepository.saveAndRefresh(record: CsipRecord): CsipRecord {
  val saved = saveAndFlush(record)
  refresh(saved)
  return saved
}

fun matchesPrisonNumber(prisonNumber: String) = Specification<CsipRecord> { csip, _, cb -> cb.equal(cb.lower(csip[PRISON_NUMBER]), prisonNumber.lowercase()) }
