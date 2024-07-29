package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Record
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

object EventFactory {
  fun createDeletedEvent(
    component: AffectedComponent,
    prisonNumber: String,
    recordUuid: UUID,
    entityUuid: UUID,
    occurredAt: LocalDateTime,
    source: Source,
  ): GenericCsipEvent? = component.deleteEvent()?.let {
    GenericCsipEvent(it, prisonNumber, recordUuid, entityUuid, it.description, occurredAt, source)
  }

  private fun AffectedComponent.deleteEvent(): DomainEventType? = when (this) {
    Record, Referral, SaferCustodyScreeningOutcome, DecisionAndActions, Investigation, Plan -> null
    ContributoryFactor -> DomainEventType.CONTRIBUTORY_FACTOR_DELETED
    Interview -> DomainEventType.INTERVIEW_DELETED
    IdentifiedNeed -> DomainEventType.IDENTIFIED_NEED_DELETED
    Review -> DomainEventType.REVIEW_DELETED
    Attendee -> DomainEventType.ATTENDEE_DELETED
  }
}