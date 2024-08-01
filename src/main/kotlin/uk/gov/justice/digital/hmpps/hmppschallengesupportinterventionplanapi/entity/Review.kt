package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.SoftDelete
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "review")
@SoftDelete
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class Review(
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id")
  val plan: Plan,

  val reviewSequence: Int,
  val reviewDate: LocalDate?,
  val recordedBy: String,
  val recordedByDisplayName: String,
  val nextReviewDate: LocalDate?,
  val csipClosedDate: LocalDate?,
  val summary: String?,

  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  val actions: Set<ReviewAction>,

  @Column(unique = true, nullable = false)
  val reviewUuid: UUID = UUID.randomUUID(),

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "review_id")
  val id: Long = 0,
) : SimpleAuditable(), Parented {
  override fun parent() = plan

  @OneToMany(mappedBy = "review", cascade = [CascadeType.ALL])
  private var attendees: MutableList<Attendee> = mutableListOf()

  fun attendees() = attendees.toList()

  fun components(): Map<AffectedComponent, Set<UUID>> = buildMap {
    put(AffectedComponent.Review, setOf(reviewUuid))
    if (attendees.isNotEmpty()) {
      put(AffectedComponent.Attendee, attendees.map { it.attendeeUuid }.toSet())
    }
  }
}

fun Review.auditDescription() =
  "Review with ${attendees().size} attendees added to plan"
