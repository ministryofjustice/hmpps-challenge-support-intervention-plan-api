package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception

import java.util.UUID

abstract class MissingPrerequisiteResourceException(msg: String) : RuntimeException(msg)

class MissingReferralException(recordUuid: UUID) :
  MissingPrerequisiteResourceException("CSIP Record with UUID: $recordUuid is missing a referral.")
