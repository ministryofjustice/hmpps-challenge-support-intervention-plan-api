package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.dto

import java.time.LocalDate

data class PrisonerDto(
  val prisonerNumber: String,
  val bookingId: Long?,
  val firstName: String,
  val middleNames: String?,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val prisonId: String?,
)
