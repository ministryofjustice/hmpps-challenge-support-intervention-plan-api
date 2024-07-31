package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.SoftDelete
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertPlanRequest
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@SoftDelete
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class Plan(
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id")
  val csipRecord: CsipRecord,

  caseManager: String,
  reasonForPlan: String,
  firstCaseReviewDate: LocalDate,

  @Id
  @Column(name = "plan_id")
  val id: Long = 0,
) : SimpleAuditable(), PropertyChangeMonitor, Parented {

  override fun parent() = csipRecord

  @PostLoad
  fun resetPropertyChanges() {
    propertyChanges = mutableSetOf()
  }

  @Transient
  override var propertyChanges: MutableSet<PropertyChange> = mutableSetOf()

  var caseManager: String = caseManager
    set(value) {
      propertyChanged(::caseManager, value)
      field = value
    }

  var reasonForPlan: String = reasonForPlan
    set(value) {
      propertyChanged(::reasonForPlan, value)
      field = value
    }

  var firstCaseReviewDate: LocalDate = firstCaseReviewDate
    set(value) {
      propertyChanged(::firstCaseReviewDate, value)
      field = value
    }

  fun upsert(request: UpsertPlanRequest): Plan = apply {
    caseManager = request.caseManager
    reasonForPlan = request.reasonForPlan
    firstCaseReviewDate = request.firstCaseReviewDate
  }

  internal fun components(): Map<AffectedComponent, Set<UUID>> = buildMap {
    put(AffectedComponent.Plan, setOf())
  }

  fun auditDescription(): String = propertyChanges.joinToString(prefix = "Updated plan ") { it.description() }
}
