package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

abstract class CsipEvent : BaseEntityEvent<CsipDomainEvent>() {
  abstract val recordUuid: UUID
  abstract val prisonNumber: String

  protected fun toDomainEvent(
    type: DomainEventType,
    baseUrl: String,
    isRecordAffected: Boolean = false,
    isReferralAffected: Boolean = false,
    isContributoryFactorAffected: Boolean = false,
    isSaferCustodyScreeningOutcomeAffected: Boolean = false,
    isInvestigationAffected: Boolean = false,
    isInterviewAffected: Boolean = false,
    isDecisionAndActionsAffected: Boolean = false,
    isPlanAffected: Boolean = false,
    isIdentifiedNeedAffected: Boolean = false,
    isReviewAffected: Boolean = false,
    isAttendeeAffected: Boolean = false,
  ): CsipDomainEvent =
    CsipDomainEvent(
      eventType = type.eventType,
      additionalInformation = CsipAdditionalInformation(
        url = "$baseUrl/csip-records/$recordUuid",
        recordUuid = recordUuid,
        prisonNumber = prisonNumber,
        isRecordAffected = isRecordAffected,
        isReferralAffected = isReferralAffected,
        isContributoryFactorAffected = isContributoryFactorAffected,
        isSaferCustodyScreeningOutcomeAffected = isSaferCustodyScreeningOutcomeAffected,
        isInvestigationAffected = isInvestigationAffected,
        isInterviewAffected = isInterviewAffected,
        isDecisionAndActionsAffected = isDecisionAndActionsAffected,
        isPlanAffected = isPlanAffected,
        isIdentifiedNeedAffected = isIdentifiedNeedAffected,
        isReviewAffected = isReviewAffected,
        isAttendeeAffected = isAttendeeAffected,
        source = source,
        reason = reason,
      ),
      description = description,
      occurredAt = occurredAt.toOffsetString(),
    )
}

data class CsipUpdatedEvent(
  override val recordUuid: UUID,
  override val prisonNumber: String,
  override val description: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  override val reason: Reason,
  val updatedBy: String,
  val isRecordAffected: Boolean = false,
  val isReferralAffected: Boolean = false,
  val isContributoryFactorAffected: Boolean = false,
  val isSaferCustodyScreeningOutcomeAffected: Boolean = false,
  val isInvestigationAffected: Boolean = false,
  val isInterviewAffected: Boolean = false,
  val isDecisionAndActionsAffected: Boolean = false,
  val isPlanAffected: Boolean = false,
  val isIdentifiedNeedAffected: Boolean = false,
  val isReviewAffected: Boolean = false,
  val isAttendeeAffected: Boolean = false,
) : CsipEvent() {
  override fun toString(): String {
    return "Updated CSIP record with UUID '$recordUuid' " +
      "for prison number '$prisonNumber' " +
      "at '$occurredAt' " +
      "by '$updatedBy' " +
      "from source '$source' " +
      "with reason '$reason'. " +
      "Properties updated: " +
      "record: $isRecordAffected, " +
      "referral: $isReferralAffected, " +
      "contributoryFactor: $isContributoryFactorAffected, " +
      "saferCustodyScreeningOutcome: $isSaferCustodyScreeningOutcomeAffected, " +
      "investigation: $isInvestigationAffected, " +
      "interview: $isInterviewAffected, " +
      "decisionAndActions: $isDecisionAndActionsAffected, " +
      "plan: $isPlanAffected, " +
      "identifiedNeed: $isIdentifiedNeedAffected, " +
      "review: $isReviewAffected, " +
      "attendee: $isAttendeeAffected."
  }

  override fun toDomainEvent(baseUrl: String): CsipDomainEvent =
    toDomainEvent(
      type = DomainEventType.CSIP_UPDATED,
      baseUrl = baseUrl,
      isRecordAffected = isRecordAffected,
      isReferralAffected = isReferralAffected,
      isContributoryFactorAffected = isContributoryFactorAffected,
      isSaferCustodyScreeningOutcomeAffected = isSaferCustodyScreeningOutcomeAffected,
      isInvestigationAffected = isInvestigationAffected,
      isInterviewAffected = isInterviewAffected,
      isDecisionAndActionsAffected = isDecisionAndActionsAffected,
      isPlanAffected = isPlanAffected,
      isIdentifiedNeedAffected = isIdentifiedNeedAffected,
      isReviewAffected = isReviewAffected,
      isAttendeeAffected = isAttendeeAffected,
    )
}

fun LocalDateTime.toOffsetString(): String =
  DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this.atOffset(ZoneId.of("Europe/London").rules.getOffset(this)))
