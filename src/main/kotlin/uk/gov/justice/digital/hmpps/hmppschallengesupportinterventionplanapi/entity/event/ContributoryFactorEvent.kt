package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

abstract class ContributoryFactorEvent : BaseEntityEvent<ContributoryFactorDomainEvent>() {
  abstract val contributoryFactorUuid: UUID
  abstract val recordUuid: UUID
  abstract val prisonNumber: String

  protected fun toDomainEvent(
    type: DomainEventType,
    baseUrl: String,
  ) = ContributoryFactorDomainEvent(
    eventType = type.eventType,
    additionalInformation = ContributoryFactorAdditionalInformation(
      url = "$baseUrl/csip-records/$recordUuid",
      contributoryFactorUuid = contributoryFactorUuid,
      recordUuid = recordUuid,
      prisonNumber = prisonNumber,
      source = source,
      reason = reason,
    ),
    description = description,
    occurredAt = occurredAt.toOffsetString(),
  )
}

data class ContributoryFactorCreatedEvent(
  override val contributoryFactorUuid: UUID,
  override val recordUuid: UUID,
  override val prisonNumber: String,
  override val description: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  override val reason: Reason,
  val updatedBy: String,
) : ContributoryFactorEvent() {
  override fun toDomainEvent(baseUrl: String): ContributoryFactorDomainEvent =
    toDomainEvent(type = DomainEventType.CONTRIBUTORY_FACTOR_CREATED, baseUrl = baseUrl)

  override fun toString(): String {
    return "Create Contributory Factor with UUID '$contributoryFactorUuid' " +
      "of CSIP record with UUID '$recordUuid' " +
      "for prison number '$prisonNumber' " +
      "at '$occurredAt' " +
      "by '$updatedBy' " +
      "from source '$source' " +
      "with reason '$reason'."
  }
}
