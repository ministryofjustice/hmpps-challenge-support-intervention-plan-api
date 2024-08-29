package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception

import java.util.UUID

abstract class MissingPrerequisiteResourceException(msg: String) : RuntimeException(msg)

interface MissingComponentException {
  val recordUuid: UUID
}

class MissingReferralException(override val recordUuid: UUID) :
  MissingPrerequisiteResourceException("CSIP Record is missing a referral."), MissingComponentException

class MissingInvestigationException(override val recordUuid: UUID) :
  MissingPrerequisiteResourceException("CSIP Record is missing an investigation."), MissingComponentException

class MissingPlanException(override val recordUuid: UUID) :
  MissingPrerequisiteResourceException("CSIP Record is missing a plan."), MissingComponentException
