package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import java.util.concurrent.atomic.AtomicLong

object IdGenerator {
  private val id = AtomicLong(1)

  fun newId() = id.getAndIncrement()
}
