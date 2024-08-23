package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.IdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, DeleteEventListener::class)
@BatchSize(size = 20)
class IdentifiedNeed(
  @Audited(withModifiedFlag = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id")
  val plan: Plan,

  identifiedNeed: String,
  responsiblePerson: String,
  createdDate: LocalDate,
  targetDate: LocalDate,
  closedDate: LocalDate?,
  intervention: String,
  progression: String?,

  legacyId: Long? = null,
) : SimpleAuditable(), Identifiable, CsipAware {
  override fun csipRecord() = plan.csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "identified_need_id")
  override val id: UUID = newUuid()

  @Audited(withModifiedFlag = false)
  override var legacyId: Long? = legacyId
    private set

  var identifiedNeed: String = identifiedNeed
    private set
  var responsiblePerson: String = responsiblePerson
    private set
  var createdDate: LocalDate = createdDate
    private set
  var targetDate: LocalDate = targetDate
    private set
  var closedDate: LocalDate? = closedDate
    private set
  var intervention: String = intervention
    private set
  var progression: String? = progression
    private set

  fun update(request: IdentifiedNeedRequest): IdentifiedNeed = apply {
    identifiedNeed = request.identifiedNeed
    responsiblePerson = request.responsiblePerson
    createdDate = request.createdDate
    targetDate = request.targetDate
    closedDate = request.closedDate
    intervention = request.intervention
    progression = request.progression
    if (request is LegacyIdAware) {
      legacyId = request.legacyId
    }
  }
}
