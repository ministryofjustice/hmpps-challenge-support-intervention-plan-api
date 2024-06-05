package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

abstract class InterviewEvent : BaseEntityEvent<InterviewDomainEvent>() {
  abstract val interviewUuid: UUID
  abstract val recordUuid: UUID
  abstract val prisonNumber: String

  protected fun toDomainEvent(
    type: DomainEventType,
    baseUrl: String,
  ) = InterviewDomainEvent(
    eventType = type.eventType,
    additionalInformation = InterviewAdditionalInformation(
      url = "$baseUrl/csip-records/$recordUuid",
      interviewUuid = interviewUuid,
      recordUuid = recordUuid,
      prisonNumber = prisonNumber,
      source = source,
      reason = reason,
    ),
    description = description,
    occurredAt = occurredAt.toOffsetString(),
  )
}

data class InterviewCreatedEvent(
  override val interviewUuid: UUID,
  override val recordUuid: UUID,
  override val prisonNumber: String,
  override val description: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  override val reason: Reason,
  val updatedBy: String,
) : InterviewEvent() {
  override fun toDomainEvent(baseUrl: String): InterviewDomainEvent =
    toDomainEvent(type = DomainEventType.INTERVIEW_CREATED, baseUrl = baseUrl)

  override fun toString(): String {
    return "Create Interview with UUID '$interviewUuid' " +
      "of CSIP record with UUID '$recordUuid' " +
      "for prison number '$prisonNumber' " +
      "at '$occurredAt' " +
      "by '$updatedBy' " +
      "from source '$source' " +
      "with reason '$reason'."
  }
}
