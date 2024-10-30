package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.SimpleVersion
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.newUuid
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.AttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.ReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(CsipChangedListener::class)
@BatchSize(size = 20)
class Review(
  @Audited(withModifiedFlag = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id")
  val plan: Plan,

  reviewSequence: Int,
  reviewDate: LocalDate?,
  recordedBy: String,
  recordedByDisplayName: String,
  nextReviewDate: LocalDate?,
  csipClosedDate: LocalDate?,
  summary: String?,
  actions: Set<ReviewAction>,

  legacyId: Long? = null,
) : SimpleVersion(), Identifiable, CsipAware {
  override fun csipRecord() = plan.csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "review_id")
  override val id: UUID = newUuid()

  @Audited(withModifiedFlag = false)
  override var legacyId: Long? = legacyId
    private set

  var reviewSequence: Int = reviewSequence
    private set
  var reviewDate: LocalDate? = reviewDate
    private set
  var recordedBy: String = recordedBy
    private set
  var recordedByDisplayName: String = recordedByDisplayName
    private set
  var nextReviewDate: LocalDate? = nextReviewDate
    private set
  var csipClosedDate: LocalDate? = csipClosedDate
    private set
  var summary: String? = summary
    private set

  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  var actions: Set<ReviewAction> = actions
    private set

  @NotAudited
  @OneToMany(mappedBy = "review", cascade = [CascadeType.ALL])
  private var attendees: MutableList<Attendee> = mutableListOf()

  fun attendees() = attendees.toList()

  fun update(request: ReviewRequest): Review = apply {
    reviewDate = request.reviewDate
    recordedBy = request.recordedBy
    recordedByDisplayName = request.recordedByDisplayName
    nextReviewDate = request.nextReviewDate
    csipClosedDate = request.csipClosedDate
    summary = request.summary
    actions = request.actions
    if (request is LegacyIdAware) {
      legacyId = request.legacyId
    }
  }

  fun updateNextReviewDate(date: LocalDate?) {
    nextReviewDate = date
  }

  fun addAttendee(request: AttendeeRequest) =
    Attendee(
      this,
      request.name,
      request.role,
      request.isAttended,
      request.contribution,
      if (request is LegacyIdAware) request.legacyId else null,
    ).apply {
      attendees.add(this)
    }
}

interface ReviewRepository : JpaRepository<Review, UUID> {
  @EntityGraph(attributePaths = ["plan", "attendees"])
  override fun findById(uuid: UUID): Optional<Review>
}

fun ReviewRepository.getReview(reviewUuid: UUID): Review =
  findById(reviewUuid).orElseThrow { NotFoundException("Review", reviewUuid.toString()) }

fun Review.toModel() = uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Review(
  id,
  reviewSequence,
  reviewDate,
  recordedBy,
  recordedByDisplayName,
  nextReviewDate,
  csipClosedDate,
  summary,
  actions,
  attendees().map { it.toModel() },
)
