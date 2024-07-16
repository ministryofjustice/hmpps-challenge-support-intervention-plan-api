package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidInputException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MultipleInvalidException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotActiveException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists

@Repository
interface ReferenceDataRepository : JpaRepository<ReferenceData, Long> {
  fun findByDomain(domain: ReferenceDataType): Collection<ReferenceData>

  fun findByDomainAndCode(domain: ReferenceDataType, code: String): ReferenceData?

  fun findByDomainAndCodeIn(domain: ReferenceDataType, code: Set<String>): Collection<ReferenceData>
}

fun ReferenceDataRepository.getOutcomeType(code: String) =
  verifyExists(findByDomainAndCode(ReferenceDataType.OUTCOME_TYPE, code)) {
    InvalidInputException(ReferenceDataType.OUTCOME_TYPE.name, code)
  }.also {
    verify(it.isActive()) { NotActiveException(ReferenceDataType.OUTCOME_TYPE.name, code) }
  }

fun ReferenceDataRepository.verifyAllReferenceData(
  type: ReferenceDataType,
  codes: Set<String>,
): Map<String, ReferenceData> {
  val referenceData = findByDomainAndCodeIn(type, codes).associateBy { it.code }
  val invalid = codes.filter { referenceData[it]?.isActive() != true }
  return when (invalid.size) {
    0 -> return referenceData
    1 -> referenceData.singleValidation(type, invalid.single())
    else -> throw MultipleInvalidException(
      type.name,
      invalid.joinToString(prefix = "[", postfix = "]", separator = ","),
    )
  }
}

private fun Map<String, ReferenceData>.singleValidation(type: ReferenceDataType, code: String) = mapOf(
  code to
    verifyExists(this[code]) {
      InvalidInputException(type.name, code)
    }.also {
      verify(it.isActive()) { NotActiveException(type.name, code) }
    },
)
