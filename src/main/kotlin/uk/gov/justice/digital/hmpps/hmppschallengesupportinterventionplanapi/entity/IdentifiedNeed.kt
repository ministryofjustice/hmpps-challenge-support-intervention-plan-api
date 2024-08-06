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
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited
@SoftDelete
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class IdentifiedNeed(
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id")
  val plan: Plan,

  @Audited(withModifiedFlag = true)
  val identifiedNeed: String,
  @Audited(withModifiedFlag = true)
  val responsiblePerson: String,
  @Audited(withModifiedFlag = true)
  val createdDate: LocalDate,
  @Audited(withModifiedFlag = true)
  val targetDate: LocalDate,
  @Audited(withModifiedFlag = true)
  val closedDate: LocalDate?,
  @Audited(withModifiedFlag = true)
  val intervention: String,
  @Audited(withModifiedFlag = true)
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
