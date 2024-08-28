package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord.Companion.CREATED_AT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord.Companion.LOG_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord.Companion.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface CsipRecordRepository :
  JpaRepository<CsipRecord, Long>,
  JpaSpecificationExecutor<CsipRecord>,
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

fun matchesPrisonNumber(prisonNumber: String) =
  Specification<CsipRecord> { csip, _, cb -> cb.equal(cb.lower(csip[PRISON_NUMBER]), prisonNumber.lowercase()) }

fun isLikeLogCode(value: String) =
  Specification<CsipRecord> { csip, _, cb -> cb.like(cb.lower(csip[LOG_CODE]), "%${value.lowercase()}%") }

fun createdBefore(to: LocalDateTime) =
  Specification<CsipRecord> { csip, _, cb -> cb.lessThanOrEqualTo(csip[CREATED_AT], to) }

fun createdAfter(from: LocalDateTime) =
  Specification<CsipRecord> { csip, _, cb -> cb.greaterThanOrEqualTo(csip[CREATED_AT], from) }
