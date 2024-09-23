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
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.AttendeesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.IdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.PlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ReviewRequest
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, CsipChangedListener::class)
class Plan(
  @Audited(withModifiedFlag = false)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id")
  val csipRecord: CsipRecord,

  caseManager: String?,
  reasonForPlan: String?,
  firstCaseReviewDate: LocalDate?,
) : SimpleAuditable(), CsipAware {
  override fun csipRecord() = csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "plan_id")
  val id: UUID = csipRecord.id

  var caseManager: String? = caseManager
    private set

  var reasonForPlan: String? = reasonForPlan
    private set

  var firstCaseReviewDate: LocalDate? = firstCaseReviewDate
    private set

  @NotAudited
  @OneToMany(mappedBy = "plan", cascade = [CascadeType.ALL])
  private var identifiedNeeds: MutableList<IdentifiedNeed> = mutableListOf()

  @NotAudited
  @OneToMany(mappedBy = "plan", cascade = [CascadeType.ALL])
  private var reviews: MutableList<Review> = mutableListOf()

  fun identifiedNeeds() = identifiedNeeds.toList()

  fun reviews() = reviews.toList()

  fun update(request: PlanRequest): Plan = apply {
    caseManager = request.caseManager
    reasonForPlan = request.reasonForPlan
    firstCaseReviewDate = request.firstCaseReviewDate
  }

  fun addIdentifiedNeed(request: IdentifiedNeedRequest): IdentifiedNeed =
    IdentifiedNeed(
      this,
      request.identifiedNeed,
      request.responsiblePerson,
      request.createdDate,
      request.targetDate,
      request.closedDate,
      request.intervention,
      request.progression,
      legacyId = if (request is LegacyIdAware) request.legacyId else null,
    ).apply {
      identifiedNeeds.add(this)
    }

  fun addReview(request: ReviewRequest): Review = Review(
    this,
    (reviews.maxOfOrNull(Review::reviewSequence) ?: 0) + 1,
    request.reviewDate,
    request.recordedBy,
    request.recordedByDisplayName,
    request.nextReviewDate,
    request.csipClosedDate,
    request.summary,
    request.actions,
    legacyId = if (request is LegacyIdAware) request.legacyId else null,
  ).apply {
    reviews.add(this)
    if (request is AttendeesRequest) {
      request.attendees.forEach { addAttendee(it) }
    }
  }

  fun nextReviewDate(): LocalDate? {
    val nextReviewDate = reviews().mapNotNull(Review::nextReviewDate).maxOrNull()
    return listOfNotNull(firstCaseReviewDate, nextReviewDate).maxOrNull()
  }

  fun components(): Set<CsipComponent> = buildSet {
    add(CsipComponent.PLAN)
    if (identifiedNeeds.isNotEmpty()) {
      add(CsipComponent.IDENTIFIED_NEED)
    }
    if (reviews.isNotEmpty()) {
      add(CsipComponent.REVIEW)
    }
    if (reviews.flatMap { it.attendees() }.isNotEmpty()) {
      add(CsipComponent.ATTENDEE)
    }
  }
}
