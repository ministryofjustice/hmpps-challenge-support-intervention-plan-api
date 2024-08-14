package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import java.util.UUID

@Repository
interface CsipRecordRepository :
  JpaRepository<CsipRecord, Long>,
  RevisionRepository<CsipRecord, Long, Long>,
  RefreshRepository<CsipRecord, Long> {
  fun findById(recordId: UUID): CsipRecord?
}

fun CsipRecordRepository.getCsipRecord(recordUuid: UUID) =
  findById(recordUuid) ?: throw NotFoundException("CSIP Record", recordUuid.toString())

fun CsipRecordRepository.saveAndRefresh(record: CsipRecord): CsipRecord {
  val saved = saveAndFlush(record)
  refresh(saved)
  return saved
}
