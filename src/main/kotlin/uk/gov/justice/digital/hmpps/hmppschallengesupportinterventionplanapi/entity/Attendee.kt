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
import java.util.UUID

@Entity
@Table
@Audited
@SoftDelete
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class Attendee(
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "review_id")
  val review: Review,

  @Audited(withModifiedFlag = true)
  val name: String?,
  @Audited(withModifiedFlag = true)
  val role: String?,
  @Audited(withModifiedFlag = true)
  val attended: Boolean?,
  @Audited(withModifiedFlag = true)
  val contribution: String?,

  @Column(unique = true, nullable = false)
  val attendeeUuid: UUID = UUID.randomUUID(),

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attendee_id")
  val id: Long = 0,
) : SimpleAuditable(), Parented {
  override fun parent() = review
}

fun Attendee.auditDescription() =
  "Added attendee '$name' with role '$role' to plan review"
