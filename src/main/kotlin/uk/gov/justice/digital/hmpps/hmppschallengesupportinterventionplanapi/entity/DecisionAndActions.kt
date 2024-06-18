package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toReferenceDataModel
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions as DecisionAndActionsModel

@Entity
@Table
data class DecisionAndActions(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "decision_and_actions_id")
  val decisionAndActionsId: Long = 0,

  @OneToOne(fetch = FetchType.LAZY) @JoinColumn(
    name = "referral_id",
    referencedColumnName = "referral_id",
  ) val referral: Referral,

  @ManyToOne @JoinColumn(
    name = "decision_outcome_id",
    referencedColumnName = "reference_data_id",
    nullable = false,
  ) val decisionOutcome: ReferenceData,

  @ManyToOne @JoinColumn(
    name = "decision_outcome_signed_off_by_role_id",
    referencedColumnName = "reference_data_id",
  ) val decisionOutcomeSignedOffBy: ReferenceData?,

  @Column(length = 4000) val decisionConclusion: String?,

  @Column(length = 100) val decisionOutcomeRecordedBy: String?,

  @Column(length = 255) val decisionOutcomeRecordedByDisplayName: String?,

  @Column val decisionOutcomeDate: LocalDate?,

  @Column(length = 4000) val nextSteps: String?,

  @Column(nullable = false) val actionOpenCsipAlert: Boolean = false,

  @Column(nullable = false) val actionNonAssociationsUpdated: Boolean = false,

  @Column(nullable = false) val actionObservationBook: Boolean = false,

  @Column(nullable = false) val actionUnitOrCellMove: Boolean = false,

  @Column(nullable = false) val actionCsraOrRsraReview: Boolean = false,

  @Column(nullable = false) val actionServiceReferral: Boolean = false,

  @Column(nullable = false) val actionSimReferral: Boolean = false,

  @Column(length = 4000) val actionOther: String?,
)

fun DecisionAndActions.toModel() =
  DecisionAndActionsModel(
    decisionConclusion,
    decisionOutcome.toReferenceDataModel(),
    decisionOutcomeSignedOffBy?.toReferenceDataModel(),
    decisionOutcomeRecordedBy,
    decisionOutcomeRecordedByDisplayName,
    decisionOutcomeDate,
    nextSteps,
    actionOpenCsipAlert,
    actionNonAssociationsUpdated,
    actionObservationBook,
    actionUnitOrCellMove,
    actionCsraOrRsraReview,
    actionServiceReferral,
    actionSimReferral,
    actionOther,
  )
