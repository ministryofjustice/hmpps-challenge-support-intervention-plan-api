package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class BehaviourType(
  @JsonValue
  val value: String,
) {
  RISKS_AND_TRIGGERS("risks_and_triggers"),
  USUAL_BEHAVIOUR_PRESENTATION("usual_behaviour_presentation"),
  PROTECTIVE_FACTORS("protective_factors"),
  ;

  companion object {
    @JsonCreator
    @JvmStatic
    fun from(value: String): BehaviourType = entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
      ?: throw IllegalArgumentException("Unknown BehaviourType value: $value")
  }
}
