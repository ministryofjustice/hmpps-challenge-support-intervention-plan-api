package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType

enum class InvalidReferenceCodeType(val message: String) {
  DOES_NOT_EXIST("does not exist"),
  IS_INACTIVE("is inactive"),
}

class InvalidReferenceDataCodeException(
  val type: InvalidReferenceCodeType,
  val domain: ReferenceDataType,
  val code: String,
) : Exception() {
  companion object {
    fun combineToIllegalArgumentException(exceptions: Collection<InvalidReferenceDataCodeException>) {
      throw exceptions.toIllegalArgumentException()
    }
  }

  fun toIllegalArgumentException() = IllegalArgumentException(
    "$domain code '$code' ${type.message}",
  )
}

fun Collection<InvalidReferenceDataCodeException>.toIllegalArgumentException() = let { exceptions ->
  if (exceptions.size == 1) {
    return exceptions.single().toIllegalArgumentException()
  }

  this.groupBy { it.domain }.map { byDomain ->
    byDomain.value.groupBy { it.type }.map { byType ->
      byType.value.joinToString(", ") { "'${it.code}'" }.let {
        "${byDomain.key} code(s) $it ${byType.key.message}"
      }
    }.joinToString(". ")
  }.joinToString(". ").let { IllegalArgumentException(it) }
}
