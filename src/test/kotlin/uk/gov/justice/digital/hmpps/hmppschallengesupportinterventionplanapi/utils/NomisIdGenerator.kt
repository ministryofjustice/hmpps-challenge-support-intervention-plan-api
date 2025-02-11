package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import java.util.concurrent.atomic.AtomicLong

object NomisIdGenerator {
  private var id = AtomicLong(1)
  private val letters = ('A'..'Z')

  fun newId(): Long = id.getAndIncrement()
  fun prisonNumber(): String = "${letters.random()}${(1111..9999).random()}${letters.random()}${letters.random()}"
  fun cellLocation(): String = "${letters.random()}-${(1..9).random()}-${(111..999).random()}"

  fun setStartingIdValue(value: Long) {
    id = AtomicLong(value)
  }
}
