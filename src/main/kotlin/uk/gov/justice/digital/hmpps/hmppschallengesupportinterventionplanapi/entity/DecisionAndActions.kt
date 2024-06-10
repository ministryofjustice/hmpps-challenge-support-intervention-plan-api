package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toReferenceDataModel
import java.time.LocalDate

@Entity
@Table
data class DecisionAndActions(
  @Id @MapsId("record_id") @OneToOne(fetch = FetchType.LAZY) @JoinColumn(
    name = "record_id",
    referencedColumnName = "record_id",
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

  @Column(nullable = false) val actionOpenCsipAlert: Boolean? = false,

  @Column(nullable = false) val actionNonAssociationsUpdated: Boolean? = false,

  @Column(nullable = false) val actionObservationBook: Boolean? = false,

  @Column(nullable = false) val actionUnitOrCellMove: Boolean? = false,

  @Column(nullable = false) val actionCsraOrRsraReview: Boolean? = false,

  @Column(nullable = false) val actionServiceReferral: Boolean? = false,

  @Column(nullable = false) val actionSimReferral: Boolean? = false,

  @Column(length = 4000) val actionOther: String?,
)

fun DecisionAndActions.toModel() =
  uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions(
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
