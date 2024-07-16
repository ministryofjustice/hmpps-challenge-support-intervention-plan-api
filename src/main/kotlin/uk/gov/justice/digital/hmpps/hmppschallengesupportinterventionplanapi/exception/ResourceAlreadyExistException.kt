package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception

class ResourceAlreadyExistException(msg: String) : RuntimeException(msg)

fun <T, E : RuntimeException> verifyDoesNotExist(value: T?, exception: () -> E) {
  if (value != null) throw exception()
}
