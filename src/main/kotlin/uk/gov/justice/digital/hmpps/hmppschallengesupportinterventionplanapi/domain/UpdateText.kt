package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import kotlin.reflect.KMutableProperty0

fun <T : String?> KMutableProperty0<T>.ifAppended(text: T) {
  if (!text.isNullOrBlank() && text.length > (get()?.length ?: 0)) {
    set(text)
  }
}
