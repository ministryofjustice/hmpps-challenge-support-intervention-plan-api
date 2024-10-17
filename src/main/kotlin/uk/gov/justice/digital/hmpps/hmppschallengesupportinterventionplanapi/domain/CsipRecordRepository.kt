package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord.Companion.CREATED_AT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord.Companion.LOG_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord.Companion.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord.Companion.STATUS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary.Companion.FIRST_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary.Companion.LAST_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
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

  fun findByLegacyId(legacyId: Long): CsipRecord?

  fun countByPrisonNumber(prisonNumber: String): Int
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

fun hasStatus(status: CsipStatus) = Specification<CsipRecord> { csip, _, cb ->
  cb.equal(csip.get<String>(STATUS), status.name)
}

fun matchesPersonSummary(prisonCode: String, query: String?) = Specification<CsipRecord> { csip, _, cb ->
  val person = csip.join<CsipRecord, PersonSummary>("personSummary", JoinType.INNER)
  val matchesPrison = prisonCode.let { cb.equal(cb.lower(person[PRISON_CODE]), it.lowercase()) }
  val matchesQuery = if (query?.isPrisonNumber() == true) {
    listOf(cb.equal(cb.lower(csip[PRISON_NUMBER]), query.lowercase()))
  } else {
    query?.split("\\s".toRegex())?.map {
      cb.or(
        cb.like(cb.lower(person[LAST_NAME]), "%${it.lowercase()}%"),
        cb.like(cb.lower(person[FIRST_NAME]), "%${it.lowercase()}%"),
      )
    }
  }?.toTypedArray() ?: arrayOf(cb.conjunction())
  cb.and(matchesPrison, *matchesQuery)
}

private fun String.isPrisonNumber() = matches(Regex("\\w\\d{4}\\w{2}"))
