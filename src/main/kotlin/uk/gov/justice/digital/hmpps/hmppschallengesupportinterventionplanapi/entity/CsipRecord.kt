package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toInitialReferralEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.BaseEntityEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipCreatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.DomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
data class CsipRecord(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "record_id")
  val recordId: Long = 0,

  @Column(unique = true, nullable = false)
  val recordUuid: UUID = UUID.randomUUID(),

  @Column(nullable = false, length = 10)
  val prisonNumber: String,

  @Column(length = 6)
  val prisonCodeWhenRecorded: String? = null,

  @Column(length = 10)
  val logNumber: String? = null,

  @Column(nullable = false)
  val createdAt: LocalDateTime,

  @Column(nullable = false, length = 32)
  val createdBy: String,

  @Column(nullable = false, length = 255)
  val createdByDisplayName: String,

  val lastModifiedAt: LocalDateTime? = null,

  @Column(length = 32)
  val lastModifiedBy: String? = null,

  @Column(length = 255)
  val lastModifiedByDisplayName: String? = null,
) : AbstractAggregateRoot<CsipRecord>() {
  @OneToMany(
    mappedBy = "csipRecord",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  @OrderBy("actioned_at DESC")
  private val auditEvents: MutableList<AuditEvent> = mutableListOf()

  @OneToOne(
    mappedBy = "csipRecord",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  var referral: Referral? = null

  fun referral() = referral

  fun auditEvents() = auditEvents.toList().sortedByDescending { it.actionedAt }

  fun addAuditEvent(
    action: AuditEventAction,
    description: String,
    actionedAt: LocalDateTime = LocalDateTime.now(),
    actionedBy: String,
    actionedByCapturedName: String,
    source: Source,
    reason: Reason,
    activeCaseLoadId: String?,
    isRecordAffected: Boolean? = false,
    isReferralAffected: Boolean? = false,
    isContributoryFactorAffected: Boolean? = false,
    isSaferCustodyScreeningOutcomeAffected: Boolean? = false,
    isInvestigationAffected: Boolean? = false,
    isInterviewAffected: Boolean? = false,
    isDecisionAndActionsAffected: Boolean? = false,
    isPlanAffected: Boolean? = false,
    isIdentifiedNeedAffected: Boolean? = false,
    isReviewAffected: Boolean? = false,
    isAttendeeAffected: Boolean? = false,
  ) = apply {
    auditEvents.add(
      AuditEvent(
        csipRecord = this,
        action = action,
        description = description,
        actionedAt = actionedAt,
        actionedBy = actionedBy,
        actionedByCapturedName = actionedByCapturedName,
        source = source,
        reason = reason,
        activeCaseLoadId = activeCaseLoadId,
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
      ),
    )
  }

  fun setReferral(referral: Referral) = apply {
    this.referral = referral
  }

  fun <T : DomainEvent<AdditionalInformation>> registerEntityEvent(entityEvent: BaseEntityEvent<T>) =
    apply { registerEvent(entityEvent) }

  fun registerCsipEvent(domainEvent: CsipEvent) = apply { registerEvent(domainEvent) }

  fun create(
    createCsipRecordRequest: CreateCsipRecordRequest,
    csipRequestContext: CsipRequestContext,
    incidentType: ReferenceData,
    incidentLocation: ReferenceData,
    referrerAreaOfWork: ReferenceData,
    incidentInvolvement: ReferenceData?,
    contributoryFactors: Map<String, ReferenceData>,
    releaseDate: LocalDate?,
    reason: Reason = Reason.USER,
    description: String = DomainEventType.CSIP_CREATED.description,
  ): CsipRecord = apply {
    referral = createCsipRecordRequest.toInitialReferralEntity(
      this,
      csipRequestContext,
      incidentType,
      incidentLocation,
      referrerAreaOfWork,
      incidentInvolvement,
      releaseDate,
    ).apply {
      createCsipRecordRequest.referral.contributoryFactors.forEach { factor ->
        this.addContributoryFactor(
          createRequest = factor,
          factorType = contributoryFactors[factor.factorTypeCode]!!,
          actionedAt = createdAt,
          actionedBy = createdBy,
          actionedByDisplayName = createdByDisplayName,
          source = csipRequestContext.source,
        )
      }
    }
    addAuditEvent(
      action = AuditEventAction.CREATED,
      description = "CSIP record created via referral with ${referral!!.contributoryFactors().size} contributory factors",
      actionedAt = createdAt,
      actionedBy = createdBy,
      actionedByCapturedName = createdByDisplayName,
      source = csipRequestContext.source,
      reason = reason,
      activeCaseLoadId = csipRequestContext.activeCaseLoadId,
      isRecordAffected = true,
      isReferralAffected = true,
      isContributoryFactorAffected = referral!!.contributoryFactors().isNotEmpty(),
    )
    registerEntityEvent(
      CsipCreatedEvent(
        recordUuid = this.recordUuid,
        prisonNumber = this.prisonNumber,
        description = description,
        occurredAt = createdAt,
        source = csipRequestContext.source,
        reason = reason,
        createdBy = createdBy,
        isRecordAffected = true,
        isReferralAffected = true,
        isContributoryFactorAffected = referral!!.contributoryFactors().isNotEmpty(),
      ),
    )
  }
}
