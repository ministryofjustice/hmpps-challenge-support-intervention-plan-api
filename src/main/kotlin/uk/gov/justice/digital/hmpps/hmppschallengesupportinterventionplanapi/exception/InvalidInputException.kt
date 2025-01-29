package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception

interface InvalidUserRequest {
  val name: String
  val value: String
}

data class InvalidInputException(override val name: String, override val value: String) :
  IllegalArgumentException("$name is invalid"),
  InvalidUserRequest

data class NotActiveException(override val name: String, override val value: String) :
  IllegalArgumentException("$name is not active"),
  InvalidUserRequest

data class MultipleInvalidException(override val name: String, override val value: String) :
  IllegalArgumentException("Multiple invalid $name"),
  InvalidUserRequest
