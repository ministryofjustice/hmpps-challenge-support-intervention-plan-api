package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import kotlin.reflect.KProperty0

data class PropertyChange(val name: String, val old: Any?, val new: Any?) {
  fun description() = "$name changed from ${valueWrapper(old)} to ${valueWrapper(new)}"
  private fun valueWrapper(value: Any?): String = when (value) {
    null -> "null"
    is Boolean, is Number -> "$value"
    else -> "'$value'"
  }
}

interface PropertyChangeMonitor {
  val propertyChanges: MutableSet<PropertyChange>
  fun propertyChanged(property: KProperty0<*>, newValue: Any?) {
    val old = property.get()
    if (old != newValue) {
      propertyChanges.add(PropertyChange(property.name, old, newValue))
    }
  }

  fun referenceDataChanged(property: KProperty0<ReferenceData?>, newValue: ReferenceData?) {
    val old = property.get()
    if (old?.code != newValue?.code) {
      propertyChanges.add(PropertyChange(property.name, old?.code, newValue?.code))
    }
  }
}
