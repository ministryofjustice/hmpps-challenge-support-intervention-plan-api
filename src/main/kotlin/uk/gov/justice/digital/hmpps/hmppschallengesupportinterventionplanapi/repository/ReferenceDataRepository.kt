package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidInputException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MultipleInvalidException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotActiveException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists

@Repository
interface ReferenceDataRepository : JpaRepository<ReferenceData, Long> {
  fun findByKeyDomain(domain: ReferenceDataType): Collection<ReferenceData>

  fun findByKey(key: ReferenceDataKey): ReferenceData?

  fun findByKeyIn(keys: Set<ReferenceDataKey>): Collection<ReferenceData>
}

fun ReferenceDataRepository.getActiveReferenceData(type: ReferenceDataType, code: String) =
  getActiveReferenceData(ReferenceDataKey(type, code))

fun ReferenceDataRepository.getActiveReferenceData(key: ReferenceDataKey) =
  verifyExists(findByKey(key)) {
    InvalidInputException(key.domain.name, key.code)
  }.also {
    verify(it.isActive()) { NotActiveException(key.domain.name, key.code) }
  }

fun ReferenceDataRepository.getScreeningOutcomeType(code: String) =
  getActiveReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, code)

fun ReferenceDataRepository.verifyAllReferenceData(
  type: ReferenceDataType,
  codes: Set<String>,
): Map<ReferenceDataKey, ReferenceData> {
  val referenceData = findByKeyIn(codes.map { ReferenceDataKey(type, it) }.toSet()).associateBy { it.key }
  val invalid = codes.filter { referenceData[ReferenceDataKey(type, it)]?.isActive() != true }
  return when (invalid.size) {
    0 -> return referenceData
    1 -> referenceData.singleValidation(ReferenceDataKey(type, invalid.first()))
    else -> throw MultipleInvalidException(
      type.name,
      invalid.joinToString(prefix = "[", postfix = "]", separator = ","),
    )
  }
}

private fun Map<ReferenceDataKey, ReferenceData>.singleValidation(key: ReferenceDataKey) = mapOf(
  key to
    verifyExists(this[key]) {
      InvalidInputException(key.domain.name, key.code)
    }.also {
      verify(it.isActive()) { NotActiveException(key.domain.name, key.code) }
    },
)
