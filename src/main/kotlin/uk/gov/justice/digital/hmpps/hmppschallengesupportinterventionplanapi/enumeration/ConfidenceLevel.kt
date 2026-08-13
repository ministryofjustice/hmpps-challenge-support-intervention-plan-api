package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class ConfidenceLevel(
  @JsonValue
  val value: String,
) {
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high"),
  ;

  companion object {
    @JsonCreator
    @JvmStatic
    fun from(value: String): ConfidenceLevel = entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
      ?: throw IllegalArgumentException("Unknown ConfidenceLevel value: $value")
  }
}
