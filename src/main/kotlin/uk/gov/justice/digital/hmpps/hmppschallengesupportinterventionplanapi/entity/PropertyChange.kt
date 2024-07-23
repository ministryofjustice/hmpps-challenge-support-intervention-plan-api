package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

data class PropertyChange(val name: String, val old: Any?, val new: Any?) {
  fun description() = "$name changed from ${valueWrapper(old)} to ${valueWrapper(new)}"
  private fun valueWrapper(value: Any?): String = when (value) {
    null -> "null"
    is Boolean, is Number -> "$value"
    else -> "'$value'"
  }
}
