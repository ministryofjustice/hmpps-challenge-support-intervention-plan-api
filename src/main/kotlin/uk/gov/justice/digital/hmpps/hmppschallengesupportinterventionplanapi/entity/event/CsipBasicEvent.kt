package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

sealed interface CsipBasicEvent : CsipBaseEvent<CsipBasicInformation> {
  val entityUuid: UUID
  val prisonNumber: String
  override fun detailPath(): String = "/csip-records/$recordUuid"
  override fun toDomainEvent(baseUrl: String) = CsipBasicDomainEvent(
    eventType = type.eventType,
    additionalInformation = CsipBasicInformation(
      entityUuid = entityUuid,
      recordUuid = recordUuid,
      source = source,
    ),
    description = description,
    occurredAt = occurredAt.toZoneDateTime(),
    detailUrl = "$baseUrl${detailPath()}",
    personReference = PersonReference.withPrisonNumber(prisonNumber),
  )
}

data class GenericCsipEvent(
  override val type: DomainEventType,
  override val prisonNumber: String,
  override val recordUuid: UUID,
  override val entityUuid: UUID,
  override val description: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
) : CsipBasicEvent

data class ContributoryFactorCreatedEvent(
  override val entityUuid: UUID,
  override val recordUuid: UUID,
  override val prisonNumber: String,
  override val description: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
) : CsipBasicEvent {
  override val type = DomainEventType.CONTRIBUTORY_FACTOR_CREATED
}

data class InterviewCreatedEvent(
  override val entityUuid: UUID,
  override val recordUuid: UUID,
  override val prisonNumber: String,
  override val description: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
) : CsipBasicEvent {
  override val type: DomainEventType = DomainEventType.INTERVIEW_CREATED
}
