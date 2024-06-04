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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.SaferCustodyScreeningOutcomeAlreadyExistException
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
  val recordUuid: UUID,

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

  @OneToOne(
    mappedBy = "csipRecord",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  private var saferCustodyScreeningOutcome: SaferCustodyScreeningOutcome? = null

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

  fun saferCustodyScreeningOutcome() = saferCustodyScreeningOutcome

  fun setSaferCustodyScreeningOutcome(screeningOutcome: SaferCustodyScreeningOutcome) = apply {
    if (referral == null) {
      throw MissingReferralException(recordUuid)
    }
    if (saferCustodyScreeningOutcome != null) {
      throw SaferCustodyScreeningOutcomeAlreadyExistException(recordUuid)
    }
    saferCustodyScreeningOutcome = screeningOutcome
  }

  fun setReferral(referral: Referral) = apply {
    this.referral = referral
  }

  fun registerCsipEvent(domainEvent: CsipEvent) = apply { registerEvent(domainEvent) }
}
