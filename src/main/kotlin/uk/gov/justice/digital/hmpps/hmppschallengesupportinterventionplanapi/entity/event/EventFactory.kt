package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.PersistenceAction
import java.util.UUID

object EventFactory {
  fun csipEvent(
    prisonNumber: String,
    action: PersistenceAction,
    recordUuid: UUID,
    affectedComponents: Set<CsipComponent>,
  ): CsipEvent {
    val context = csipRequestContext()
    val eventType = "${RECORD.description}.${action.name.lowercase()}"
    return CsipEvent(
      DomainEventType.fromEventName(eventType)
        ?: throw IllegalArgumentException("Unknown Event Type: $eventType"),
      prisonNumber = prisonNumber,
      recordUuid = recordUuid,
      affectedComponents = affectedComponents,
      source = context.source,
      occurredAt = context.requestAt,
    )
  }

  fun csipChildEvent(
    prisonNumber: String,
    component: CsipComponent,
    action: PersistenceAction,
    recordUuid: UUID,
    entityUuid: UUID,
  ): CsipChildEvent {
    val context = csipRequestContext()
    return CsipChildEvent(
      DomainEventType.fromEventName("${component.description}.${action.name.lowercase()}")
        ?: throw IllegalArgumentException("Unknown Event Type"),
      prisonNumber = prisonNumber,
      recordUuid = recordUuid,
      entityUuid = entityUuid,
      source = context.source,
      occurredAt = context.requestAt,
    )
  }
}
