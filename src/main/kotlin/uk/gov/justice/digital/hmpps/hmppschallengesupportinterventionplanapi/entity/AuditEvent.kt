package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime

@Entity
@Table
class AuditEvent(
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  val id: Long = 0,

  @ManyToOne
  @JoinColumn(name = "csip_record_id", referencedColumnName = "record_id", nullable = false)
  val csipRecord: CsipRecord,

  @Enumerated(EnumType.STRING)
  val action: AuditEventAction,

  val description: String,
  val actionedAt: LocalDateTime,
  val actionedBy: String,
  val actionedByCapturedName: String,

  @Enumerated(EnumType.STRING)
  val source: Source,
  @Enumerated(EnumType.STRING)
  val reason: Reason,

  val activeCaseLoadId: String?,

  @Column(name = "record_affected")
  val isRecordAffected: Boolean? = false,
  @Column(name = "referral_affected")
  val isReferralAffected: Boolean? = false,
  @Column(name = "contributory_factor_affected")
  val isContributoryFactorAffected: Boolean? = false,
  @Column(name = "safer_custody_screening_outcome_affected")
  val isSaferCustodyScreeningOutcomeAffected: Boolean? = false,
  @Column(name = "investigation_affected")
  val isInvestigationAffected: Boolean? = false,
  @Column(name = "interview_affected")
  val isInterviewAffected: Boolean? = false,
  @Column(name = "decisions_and_actions_affected")
  val isDecisionAndActionsAffected: Boolean? = false,
  @Column(name = "plan_affected")
  val isPlanAffected: Boolean? = false,
  @Column(name = "identified_need_affected")
  val isIdentifiedNeedAffected: Boolean? = false,
  @Column(name = "review_affected")
  val isReviewAffected: Boolean? = false,
  @Column(name = "attendee_affected")
  val isAttendeeAffected: Boolean? = false,
)
