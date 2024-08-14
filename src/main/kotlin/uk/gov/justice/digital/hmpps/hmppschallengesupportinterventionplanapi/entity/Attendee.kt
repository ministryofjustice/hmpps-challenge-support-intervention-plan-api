package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
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
) : SimpleAuditable(), Identifiable, CsipAware {
  override fun csipRecord() = review.plan.csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "attendee_id")
  override val id: UUID = newUuid()
}
