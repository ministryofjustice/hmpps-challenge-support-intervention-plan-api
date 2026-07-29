package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class BehaviourType(
  @JsonValue
  val value: Int,
) {
  RISKS_AND_TRIGGERS(0),
  USUAL_BEHAVIOUR_PRESENTATION(1),
  PROTECTIVE_FACTORS(2),
  ;

  companion object {
    @JsonCreator
    @JvmStatic
    fun from(value: Int): BehaviourType = entries.firstOrNull { it.value == value }
      ?: throw IllegalArgumentException("Unknown BehaviourType value: $value")
  }
}
