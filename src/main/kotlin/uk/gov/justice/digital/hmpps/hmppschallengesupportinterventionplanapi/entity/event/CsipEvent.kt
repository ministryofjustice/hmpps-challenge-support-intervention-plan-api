package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

sealed interface CsipEvent : CsipBaseEvent<CsipAdditionalInformation> {
  val prisonNumber: String
  val affectedComponents: Set<AffectedComponent>

  override fun toDomainEvent(baseUrl: String): DomainEvent =
    toDomainEvent(baseUrl, affectedComponents)

  fun toDomainEvent(baseUrl: String, affectedComponents: Set<AffectedComponent>): DomainEvent =
    CsipDomainEvent(
      eventType = type.eventType,
      additionalInformation = CsipAdditionalInformation(
        recordUuid = recordUuid,
        affectedComponents = affectedComponents,
        source = source,
      ),
      description = description,
      occurredAt = occurredAt.toZoneDateTime(),
      detailUrl = "$baseUrl${detailPath()}",
      personReference = PersonReference.withPrisonNumber(prisonNumber),
    )
}

data class CsipUpdatedEvent(
  override val recordUuid: UUID,
  override val prisonNumber: String,
  override val description: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  override val affectedComponents: Set<AffectedComponent>,
) : CsipEvent {
  override val type: DomainEventType = DomainEventType.CSIP_UPDATED
}

data class CsipCreatedEvent(
  override val recordUuid: UUID,
  override val prisonNumber: String,
  override val description: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  override val affectedComponents: Set<AffectedComponent>,
) : CsipEvent {
  override val type: DomainEventType = DomainEventType.CSIP_CREATED
}
