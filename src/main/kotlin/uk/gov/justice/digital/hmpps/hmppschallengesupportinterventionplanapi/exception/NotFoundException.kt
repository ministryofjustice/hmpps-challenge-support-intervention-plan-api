package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import java.util.UUID

data class NotFoundException(val resource: String, val identifier: String) : RuntimeException("$resource not found")

fun <E : RuntimeException> verify(boolean: Boolean, exception: () -> E) {
  if (!boolean) throw exception()
}

fun <T, E : RuntimeException> verifyExists(value: T?, exception: () -> E): T {
  return value ?: throw exception()
}

fun verifyCsipRecordExists(csipRecordRepository: CsipRecordRepository, uuid: UUID) =
  verifyExists(csipRecordRepository.findById(uuid)) {
    NotFoundException("CSIP Record", uuid.toString())
  }
