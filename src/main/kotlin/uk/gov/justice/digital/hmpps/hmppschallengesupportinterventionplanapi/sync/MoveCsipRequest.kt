package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import java.util.UUID

data class MoveCsipRequest(
  val fromPrisonNumber: String,
  val toPrisonNumber: String,
  val recordUuids: Set<UUID>,
)
