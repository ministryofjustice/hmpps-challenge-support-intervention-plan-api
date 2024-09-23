package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
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
) : CsipBaseEvent {
  override fun additionalInformation(): AdditionalInformation = CsipInformation(recordUuid)
}

interface CsipBaseInformation : AdditionalInformation {
  val recordUuid: UUID
}

data class CsipInformation(
  override val recordUuid: UUID,
) : CsipBaseInformation
