package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan

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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.SimpleVersion
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.AttendeesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.FirstReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.IdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.PlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.ReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(CsipChangedListener::class)
class Plan(
  @Audited(withModifiedFlag = false)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id")
  val csipRecord: CsipRecord,

  caseManager: String?,
  reasonForPlan: String?,
  firstCaseReviewDate: LocalDate?,
) : SimpleVersion(), CsipAware {
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
    when {
      request is FirstReviewRequest -> firstCaseReviewDate = request.firstCaseReviewDate
      reviews.isEmpty() -> firstCaseReviewDate = request.nextCaseReviewDate
      else -> {
        reviews.maxByOrNull { it.reviewSequence }!!.updateNextReviewDate(request.nextCaseReviewDate)
      }
    }
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
    val nextReviewDate = reviews().maxByOrNull { it.reviewSequence }?.nextReviewDate
    return listOfNotNull(nextReviewDate, firstCaseReviewDate).firstOrNull()
  }
}

fun Plan.toModel() = uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Plan(
  caseManager,
  reasonForPlan,
  firstCaseReviewDate,
  nextReviewDate(),
  identifiedNeeds().map { it.toModel() },
  reviews().map { it.toModel() },
)
