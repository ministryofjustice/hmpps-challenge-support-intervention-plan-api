package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

data class AffectedComponents(
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
)

interface CsipEvent : CsipBaseEvent<CsipAdditionalInformation> {
  val prisonNumber: String
  val affectedComponents: AffectedComponents

  override fun toDomainEvent(baseUrl: String): DomainEvent<CsipAdditionalInformation> =
    toDomainEvent(baseUrl, affectedComponents)

  fun toDomainEvent(baseUrl: String, affectedComponents: AffectedComponents): DomainEvent<CsipAdditionalInformation> =
    CsipDomainEvent(
      eventType = type.eventType,
      additionalInformation = CsipAdditionalInformation(
        recordUuid = recordUuid,
        isRecordAffected = affectedComponents.isRecordAffected,
        isReferralAffected = affectedComponents.isReferralAffected,
        isContributoryFactorAffected = affectedComponents.isContributoryFactorAffected,
        isSaferCustodyScreeningOutcomeAffected = affectedComponents.isSaferCustodyScreeningOutcomeAffected,
        isInvestigationAffected = affectedComponents.isInvestigationAffected,
        isInterviewAffected = affectedComponents.isInterviewAffected,
        isDecisionAndActionsAffected = affectedComponents.isDecisionAndActionsAffected,
        isPlanAffected = affectedComponents.isPlanAffected,
        isIdentifiedNeedAffected = affectedComponents.isIdentifiedNeedAffected,
        isReviewAffected = affectedComponents.isReviewAffected,
        isAttendeeAffected = affectedComponents.isAttendeeAffected,
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
  val updatedBy: String,
  override val affectedComponents: AffectedComponents,
) : CsipEvent {
  override val type: DomainEventType = DomainEventType.CSIP_UPDATED
}

data class CsipCreatedEvent(
  override val recordUuid: UUID,
  override val prisonNumber: String,
  override val description: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  val createdBy: String,
  override val affectedComponents: AffectedComponents,
) : CsipEvent {
  override val type: DomainEventType = DomainEventType.CSIP_CREATED
}
