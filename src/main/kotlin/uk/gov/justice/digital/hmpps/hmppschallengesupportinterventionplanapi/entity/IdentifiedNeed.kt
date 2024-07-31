package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SoftDelete
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "identified_need")
@SoftDelete
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class IdentifiedNeed(
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id")
  val plan: Plan,

  val identifiedNeed: String,
  val needIdentifiedBy: String,
  val createdDate: LocalDate,
  val targetDate: LocalDate,
  val closedDate: LocalDate?,
  val intervention: String,
  val progression: String?,

  @Column(unique = true, nullable = false)
  val identifiedNeedUuid: UUID = UUID.randomUUID(),

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "identified_need_id")
  val id: Long = 0,
) : SimpleAuditable(), Parented {
  override fun parent() = plan
}

fun IdentifiedNeed.auditDescription() =
  "Added identified need '$identifiedNeed' to plan"
