package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception

import jakarta.persistence.EntityNotFoundException

class CsipRecordNotFoundException(message: String) : EntityNotFoundException(message)
