package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.SimpleVersion
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.DecisionAndActionsRequest
import java.time.LocalDate
import java.util.SortedSet
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.DecisionAndActions as DecisionAndActionsModel

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(CsipChangedListener::class)
class DecisionAndActions(
  @Audited(withModifiedFlag = false)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "decision_and_actions_id")
  val referral: Referral,

  outcome: ReferenceData?,
  signedOffBy: ReferenceData?,
) : SimpleVersion(),
  CsipAware {
  override fun csipRecord() = referral.csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "decision_and_actions_id")
  val id: UUID = referral.id

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "outcome_id", nullable = false)
  var outcome: ReferenceData? = outcome
    private set

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "signed_off_by_role_id")
  var signedOffBy: ReferenceData? = signedOffBy
    private set

  @Column(length = 4000)
  var conclusion: String? = null
    private set

  @Column(length = 100)
  var recordedBy: String? = null
    private set

  @Column(length = 255)
  var recordedByDisplayName: String? = null
    private set

  @Column
  var date: LocalDate? = null
    private set

  @Column(length = 4000)
  var nextSteps: String? = null
    private set

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.ARRAY)
  var actions: SortedSet<DecisionAction> = sortedSetOf()
    private set

  @Column(length = 4000)
  var actionOther: String? = null
    private set

  fun upsert(
    request: DecisionAndActionsRequest,
    outcomeType: ReferenceData?,
    signedOffByRole: ReferenceData?,
  ): DecisionAndActions = apply {
    outcome = outcomeType
    signedOffBy = signedOffByRole
    conclusion = request.conclusion
    recordedBy = request.recordedBy
    recordedByDisplayName = request.recordedByDisplayName
    date = request.date
    nextSteps = request.nextSteps
    actions = request.actions
    actionOther = request.actionOther
  }
}

fun DecisionAndActions.toModel() = DecisionAndActionsModel(
  conclusion,
  outcome?.toReferenceDataModel(),
  signedOffBy?.toReferenceDataModel(),
  recordedBy,
  recordedByDisplayName,
  date,
  nextSteps,
  actions,
  actionOther,
)
