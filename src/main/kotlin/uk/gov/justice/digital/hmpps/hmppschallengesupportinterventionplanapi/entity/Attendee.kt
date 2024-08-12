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
import org.hibernate.envers.Audited
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, DeleteEventListener::class)
class Attendee(
  @Audited(withModifiedFlag = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "review_id")
  val review: Review,

  val name: String?,
  val role: String?,
  val attended: Boolean?,
  val contribution: String?,

  @Audited(withModifiedFlag = false)
  @Column(name = "attendee_uuid", unique = true, nullable = false)
  override val uuid: UUID = UUID.randomUUID(),

  @Audited(withModifiedFlag = false)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attendee_id")
  val id: Long = 0,
) : SimpleAuditable(), Identifiable, CsipAware {
  override fun csipRecord() = review.plan.csipRecord
}
