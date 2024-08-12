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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.DeleteEventListener
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, DeleteEventListener::class)
class IdentifiedNeed(
  @Audited(withModifiedFlag = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id")
  val plan: Plan,

  val identifiedNeed: String,
  val responsiblePerson: String,
  val createdDate: LocalDate,
  val targetDate: LocalDate,
  val closedDate: LocalDate?,
  val intervention: String,
  val progression: String?,

  @Audited(withModifiedFlag = false)
  @Column(name = "identifiedNeedUuid", unique = true, nullable = false)
  override val uuid: UUID = UUID.randomUUID(),

  @Audited(withModifiedFlag = false)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "identified_need_id")
  val id: Long = 0,
) : SimpleAuditable(), Identifiable, CsipAware {
  override fun csipRecord() = plan.csipRecord
}
