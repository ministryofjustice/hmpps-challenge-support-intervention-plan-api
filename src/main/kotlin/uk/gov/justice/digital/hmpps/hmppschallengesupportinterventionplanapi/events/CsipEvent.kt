package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.DomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.PersonReference
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
  val previousPrisonNumber: String?
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
  override val previousPrisonNumber: String? = null,
) : CsipBaseEvent {
  override fun additionalInformation(): AdditionalInformation =
    previousPrisonNumber?.let { CsipMovedInformation(recordUuid, previousPrisonNumber) } ?: CsipInformation(recordUuid)
}

interface CsipBaseInformation : AdditionalInformation {
  val recordUuid: UUID
}

data class CsipInformation(
  override val recordUuid: UUID,
) : CsipBaseInformation

data class CsipMovedInformation(
  override val recordUuid: UUID,
  val previousNomsNumber: String,
) : CsipBaseInformation
