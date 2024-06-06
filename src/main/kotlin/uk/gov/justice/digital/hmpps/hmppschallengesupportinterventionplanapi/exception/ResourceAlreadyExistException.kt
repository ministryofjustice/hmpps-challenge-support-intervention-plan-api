package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception

import java.util.UUID

abstract class ResourceAlreadyExistException(msg: String) : RuntimeException(msg)

class SaferCustodyScreeningOutcomeAlreadyExistException(recordUuid: UUID) :
  ResourceAlreadyExistException("CSIP Record with UUID: $recordUuid already has a Safer Custody Screening Outcome created.")

class InvestigationAlreadyExistsException(recordUuid: UUID) :
  ResourceAlreadyExistException("CSIP Record with UUID: $recordUuid already has an Investigation created.")
