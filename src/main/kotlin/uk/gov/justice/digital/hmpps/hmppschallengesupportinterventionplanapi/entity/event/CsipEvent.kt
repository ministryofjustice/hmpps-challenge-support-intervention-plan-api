package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

sealed interface DomainEventable {
  val type: DomainEventType
  fun detailPath(): String
  fun additionalInformation(): AdditionalInformation
  fun toDomainEvent(baseUrl: String): DomainEvent
}

sealed interface CsipBaseEvent : DomainEventable {
  val occurredAt: LocalDateTime
  val source: Source
  val prisonNumber: String
  val recordUuid: UUID
  override fun detailPath(): String = "/csip-records/$recordUuid"
  override fun toDomainEvent(baseUrl: String): DomainEvent =
    HmppsDomainEvent(
      eventType = type.eventType,
      additionalInformation = additionalInformation(),
      description = type.description,
      occurredAt = occurredAt.toZoneDateTime(),
      detailUrl = "$baseUrl${detailPath()}",
      personReference = PersonReference.withPrisonNumber(prisonNumber),
    )
}

data class CsipEvent(
  override val type: DomainEventType,
  override val prisonNumber: String,
  override val recordUuid: UUID,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  val affectedComponents: Set<CsipComponent>,
) : CsipBaseEvent {
  override fun additionalInformation(): AdditionalInformation =
    CsipInformation(source, recordUuid, affectedComponents)
}

data class CsipChildEvent(
  override val type: DomainEventType,
  override val prisonNumber: String,
  override val recordUuid: UUID,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  val entityUuid: UUID,
) : CsipBaseEvent {
  override fun additionalInformation(): CsipChildInformation =
    CsipChildInformation(source, recordUuid, entityUuid)
}

interface CsipBaseInformation : AdditionalInformation {
  val source: Source
  val recordUuid: UUID
}

data class CsipInformation(
  override val source: Source,
  override val recordUuid: UUID,
  val affectedComponents: Set<CsipComponent>,
) : CsipBaseInformation

class CsipChildInformation(
  override val source: Source,
  override val recordUuid: UUID,
  val entityUuid: UUID,
) : CsipBaseInformation
