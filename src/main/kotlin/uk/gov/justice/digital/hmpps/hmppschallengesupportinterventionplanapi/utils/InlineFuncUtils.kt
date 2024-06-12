package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

inline fun <T, K, V, reified E : Exception> Iterable<T>.associateByAndHandleException(
  keySelector: (T) -> K,
  valueTransform: (T) -> V,
  exceptionsHandler: (Collection<E>) -> Unit,
): Map<K, V> {
  val destination = HashMap<K, V>()
  val exceptions = ArrayList<E>()
  for (element in this) {
    try {
      destination[keySelector(element)] = valueTransform(element)
    } catch (e: Exception) {
      if (e is E) {
        exceptions.add(e)
      } else {
        throw e
      }
    }
  }

  if (exceptions.isNotEmpty()) {
    exceptionsHandler(exceptions)
  }

  return destination
}
