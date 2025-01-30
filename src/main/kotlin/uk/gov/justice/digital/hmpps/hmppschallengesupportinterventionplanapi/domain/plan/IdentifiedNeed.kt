package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan

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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.SimpleVersion
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.newUuid
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.IdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(CsipChangedListener::class)
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
) : SimpleVersion(),
  Identifiable,
  CsipAware {
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

  fun closeNeedIfOpen(date: LocalDate) {
    closedDate = closedDate ?: date
  }
}

interface IdentifiedNeedRepository : JpaRepository<IdentifiedNeed, UUID>

fun IdentifiedNeedRepository.getIdentifiedNeed(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Identified Need", id.toString())

fun IdentifiedNeed.toModel() = uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.IdentifiedNeed(
  id,
  identifiedNeed,
  responsiblePerson,
  createdDate,
  targetDate,
  closedDate,
  intervention,
  progression,
)
