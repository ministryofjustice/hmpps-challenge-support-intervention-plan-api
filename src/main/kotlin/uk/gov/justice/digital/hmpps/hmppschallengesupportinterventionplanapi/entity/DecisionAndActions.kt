package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction.CsraOrRsraReview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction.NonAssociationsUpdated
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction.ObservationBook
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction.OpenCsipAlert
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction.ServiceReferral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction.SimReferral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction.UnitOrCellMove
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions as DecisionAndActionsModel

@Entity
@Table
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class DecisionAndActions(
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "referral_id", referencedColumnName = "referral_id")
  val referral: Referral,

  @ManyToOne
  @JoinColumn(name = "outcome_id", nullable = false)
  val outcome: ReferenceData,

  @ManyToOne
  @JoinColumn(name = "outcome_signed_off_by_role_id")
  val outcomeSignedOffBy: ReferenceData?,

  @Column(length = 4000) val conclusion: String?,
  @Column(length = 100) val outcomeRecordedBy: String?,
  @Column(length = 255) val outcomeRecordedByDisplayName: String?,
  @Column val outcomeDate: LocalDate?,
  @Column(length = 4000) val nextSteps: String?,

  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  val actions: Set<DecisionAction>,
  @Column(length = 4000) val actionOther: String?,

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "decision_and_actions_id")
  val id: Long = 0,
) : SimpleAuditable(), Parented {
  override fun parent() = referral
}

fun DecisionAndActions.toModel() =
  DecisionAndActionsModel(
    conclusion,
    outcome.toReferenceDataModel(),
    outcomeSignedOffBy?.toReferenceDataModel(),
    outcomeRecordedBy,
    outcomeRecordedByDisplayName,
    outcomeDate,
    nextSteps,
    OpenCsipAlert in actions,
    NonAssociationsUpdated in actions,
    ObservationBook in actions,
    UnitOrCellMove in actions,
    CsraOrRsraReview in actions,
    ServiceReferral in actions,
    SimReferral in actions,
    actionOther,
  )
