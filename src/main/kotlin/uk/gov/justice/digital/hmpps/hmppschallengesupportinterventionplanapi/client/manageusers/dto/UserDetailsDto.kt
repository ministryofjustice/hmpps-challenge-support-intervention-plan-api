package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.manageusers.dto

import java.util.UUID

data class UserDetailsDto(
  val username: String,
  val active: Boolean,
  val name: String,
  val authSource: String,
  val userId: String,
  val uuid: UUID?,
)
