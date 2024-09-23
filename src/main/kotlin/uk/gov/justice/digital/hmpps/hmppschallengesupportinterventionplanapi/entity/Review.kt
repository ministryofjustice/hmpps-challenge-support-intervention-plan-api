package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.AttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ReviewRequest
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, CsipChangedListener::class)
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
) : SimpleAuditable(), Identifiable, CsipAware {
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
