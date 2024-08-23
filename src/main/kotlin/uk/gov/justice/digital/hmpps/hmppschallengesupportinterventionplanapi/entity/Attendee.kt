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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.AttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, DeleteEventListener::class)
@BatchSize(size = 20)
class Attendee(
  @Audited(withModifiedFlag = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "review_id")
  val review: Review,

  name: String?,
  role: String?,
  attended: Boolean?,
  contribution: String?,

  legacyId: Long? = null,
) : SimpleAuditable(), Identifiable, CsipAware {
  override fun csipRecord() = review.plan.csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "attendee_id")
  override val id: UUID = newUuid()

  @Audited(withModifiedFlag = false)
  override var legacyId: Long? = legacyId
    private set

  var name: String? = name
    private set
  var role: String? = role
    private set
  var attended: Boolean? = attended
    private set
  var contribution: String? = contribution
    private set

  fun update(request: AttendeeRequest): Attendee = apply {
    name = request.name
    role = request.role
    attended = request.isAttended
    contribution = request.contribution
    if (request is LegacyIdAware) {
      legacyId = request.legacyId
    }
  }
}
