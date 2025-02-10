package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import kotlin.reflect.KProperty0

// Only to be used in tests to help instantiate entities
fun <T : Any, V : Any?> T.set(field: KProperty0<V>, value: V): T = setByName(field.name, value)

fun <T : Any, V : Any?> T.setByName(field: String, value: V): T {
  val f = this::class.java.getDeclaredField(field)
  f.isAccessible = true
  f[this] = value
  f.isAccessible = false
  return this
}

inline fun <reified V : Any?> Any.getByName(field: String): V {
  val f = this::class.java.getDeclaredField(field)
  f.isAccessible = true
  val value = f[this] as V
  f.isAccessible = false
  return value
}
