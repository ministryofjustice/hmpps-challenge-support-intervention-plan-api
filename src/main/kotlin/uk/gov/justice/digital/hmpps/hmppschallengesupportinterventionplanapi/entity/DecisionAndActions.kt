package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.SoftDelete
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions as DecisionAndActionsModel

@Entity
@Table
@SoftDelete
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class DecisionAndActions(
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "decision_and_actions_id")
  val referral: Referral,

  outcome: ReferenceData,

  @Id
  @Column(name = "decision_and_actions_id")
  val id: Long = 0,
) : SimpleAuditable(), Parented, PropertyChangeMonitor {
  override fun parent() = referral

  @PostLoad
  fun resetPropertyChanges() {
    propertyChanges = mutableSetOf()
  }

  @Transient
  override var propertyChanges: MutableSet<PropertyChange> = mutableSetOf()

  @ManyToOne
  @JoinColumn(name = "outcome_id", nullable = false)
  var outcome: ReferenceData = outcome
    set(value) {
      referenceDataChanged(::outcome, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "signed_off_by_role_id")
  var signedOffBy: ReferenceData? = null
    set(value) {
      referenceDataChanged(::signedOffBy, value)
      field = value
    }

  @Column(length = 4000)
  var conclusion: String? = null
    set(value) {
      propertyChanged(::conclusion, value)
      field = value
    }

  @Column(length = 100)
  var recordedBy: String? = null
    set(value) {
      propertyChanged(::recordedBy, value)
      field = value
    }

  @Column(length = 255)
  var recordedByDisplayName: String? = null
    set(value) {
      propertyChanged(::recordedByDisplayName, value)
      field = value
    }

  @Column
  var date: LocalDate? = null
    set(value) {
      propertyChanged(::date, value)
      field = value
    }

  @Column(length = 4000)
  var nextSteps: String? = null
    set(value) {
      propertyChanged(::nextSteps, value)
      field = value
    }

  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  var actions: Set<DecisionAction> = setOf()
    set(value) {
      propertyChanged(::actions, value)
      field = value
    }

  @Column(length = 4000)
  var actionOther: String? = null
    set(value) {
      propertyChanged(::actionOther, value)
      field = value
    }
}

fun DecisionAndActions.toModel() =
  DecisionAndActionsModel(
    conclusion,
    outcome.toReferenceDataModel(),
    signedOffBy?.toReferenceDataModel(),
    recordedBy,
    recordedByDisplayName,
    date,
    nextSteps,
    actions,
    actionOther,
  )
