package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception

import java.util.UUID

class DecisionAndActionsAlreadyExistException(recordUuid: UUID) :
  ResourceAlreadyExistException("CSIP Record with UUID: $recordUuid already has a Decision and Actions created.")
