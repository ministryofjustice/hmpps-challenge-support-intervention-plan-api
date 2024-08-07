package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.GenericCsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.IDENTIFIED_NEED_CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.REVIEW_CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.PlanRequest
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class)
class Plan(
  @Audited(withModifiedFlag = false)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id")
  val csipRecord: CsipRecord,

  caseManager: String,
  reasonForPlan: String,
  firstCaseReviewDate: LocalDate,

  @Audited(withModifiedFlag = false)
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
  @NotAudited
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

  @NotAudited
  @OneToMany(mappedBy = "plan", cascade = [CascadeType.ALL])
  private var identifiedNeeds: MutableList<IdentifiedNeed> = mutableListOf()

  @NotAudited
  @OneToMany(mappedBy = "plan", cascade = [CascadeType.ALL])
  private var reviews: MutableList<Review> = mutableListOf()

  fun identifiedNeeds() = identifiedNeeds.toList()

  fun reviews() = reviews.toList()

  fun upsert(request: PlanRequest): Plan = apply {
    caseManager = request.caseManager
    reasonForPlan = request.reasonForPlan
    firstCaseReviewDate = request.firstCaseReviewDate
  }

  internal fun components(): Map<AffectedComponent, Set<UUID>> = buildMap {
    put(AffectedComponent.Plan, setOf())
    if (identifiedNeeds.isNotEmpty()) {
      put(AffectedComponent.IdentifiedNeed, identifiedNeeds.map { it.identifiedNeedUuid }.toSet())
    }
    putAll(
      reviews.flatMap { it.components().entries }.fold(mutableMapOf()) { acc, next ->
        acc[next.component1()] = (acc[next.component1()] ?: setOf()) + (next.component2())
        acc
      },
    )
  }

  fun addIdentifiedNeed(context: CsipRequestContext, request: CreateIdentifiedNeedRequest): IdentifiedNeed =
    IdentifiedNeed(
      this,
      request.identifiedNeed,
      request.responsiblePerson,
      request.createdDate,
      request.targetDate,
      request.closedDate,
      request.intervention,
      request.progression,
    ).apply {
      verify(identifiedNeeds.none { it.identifiedNeed == identifiedNeed }) {
        ResourceAlreadyExistException("Identified need already part of plan")
      }
      identifiedNeeds.add(this)
      csipRecord.registerEntityEvent(
        GenericCsipEvent(
          type = IDENTIFIED_NEED_CREATED,
          entityUuid = identifiedNeedUuid,
          recordUuid = csipRecord.recordUuid,
          prisonNumber = csipRecord.prisonNumber,
          occurredAt = context.requestAt,
          source = context.source,
        ),
      )
    }

  fun addReview(context: CsipRequestContext, request: CreateReviewRequest): Review = Review(
    this,
    (reviews.maxOfOrNull(Review::reviewSequence) ?: 0) + 1,
    request.reviewDate,
    request.recordedBy,
    request.recordedByDisplayName,
    request.nextReviewDate,
    request.csipClosedDate,
    request.summary,
    request.actions ?: setOf(),
  ).apply {
    reviews.add(this)
    request.attendees?.forEach { addAttendee(context, it) }
    csipRecord.registerEntityEvent(
      GenericCsipEvent(
        type = REVIEW_CREATED,
        entityUuid = reviewUuid,
        recordUuid = csipRecord.recordUuid,
        prisonNumber = csipRecord.prisonNumber,
        occurredAt = context.requestAt,
        source = context.source,
      ),
    )
  }
}
