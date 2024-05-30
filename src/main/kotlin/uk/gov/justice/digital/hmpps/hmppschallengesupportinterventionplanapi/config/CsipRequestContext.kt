package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime

data class CsipRequestContext(
  val requestAt: LocalDateTime = LocalDateTime.now(),
  val source: Source = Source.DPS,
  val username: String,
  val userDisplayName: String,
)
