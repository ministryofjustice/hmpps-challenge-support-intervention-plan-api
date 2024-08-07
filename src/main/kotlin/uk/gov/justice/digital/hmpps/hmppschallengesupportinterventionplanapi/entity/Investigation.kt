package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.InterviewCreatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.InvestigationRequest
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class)
class Investigation(
  @Audited(withModifiedFlag = false)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "investigation_id")
  val referral: Referral,

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "investigation_id")
  val id: Long = 0,
) : SimpleAuditable(), Parented, PropertyChangeMonitor {

  @PostLoad
  fun resetPropertyChanges() {
    propertyChanges = mutableSetOf()
  }

  @Transient
  @NotAudited
  override var propertyChanges: MutableSet<PropertyChange> = mutableSetOf()

  override fun parent() = referral

  var staffInvolved: String? = null
    private set(value) {
      propertyChanged(::staffInvolved, value)
      field = value
    }

  var evidenceSecured: String? = null
    private set(value) {
      propertyChanged(::evidenceSecured, value)
      field = value
    }

  var occurrenceReason: String? = null
    private set(value) {
      propertyChanged(::occurrenceReason, value)
      field = value
    }

  var personsUsualBehaviour: String? = null
    private set(value) {
      propertyChanged(::personsUsualBehaviour, value)
      field = value
    }

  var personsTrigger: String? = null
    private set(value) {
      propertyChanged(::personsTrigger, value)
      field = value
    }

  var protectiveFactors: String? = null
    private set(value) {
      propertyChanged(::protectiveFactors, value)
      field = value
    }

  @NotAudited
  @OneToMany(mappedBy = "investigation", cascade = [CascadeType.ALL])
  private val interviews: MutableList<Interview> = mutableListOf()

  fun interviews() = interviews.toList().sortedByDescending { it.id }

  fun addInterview(
    context: CsipRequestContext,
    createRequest: CreateInterviewRequest,
    roleProvider: (String) -> ReferenceData,
  ) = Interview(
    investigation = this,
    interviewee = createRequest.interviewee,
    interviewDate = createRequest.interviewDate,
    intervieweeRole = roleProvider.invoke(createRequest.intervieweeRoleCode),
    interviewText = createRequest.interviewText,
  ).apply {
    interviews.add(this)
    referral.csipRecord.registerEntityEvent(
      InterviewCreatedEvent(
        entityUuid = interviewUuid,
        recordUuid = referral.csipRecord.recordUuid,
        prisonNumber = referral.csipRecord.prisonNumber,
        description = DomainEventType.INTERVIEW_CREATED.description,
        occurredAt = context.requestAt,
        source = context.source,
      ),
    )
  }

  internal fun components(): Map<AffectedComponent, Set<UUID>> = buildMap {
    put(AffectedComponent.Investigation, setOf())
    if (interviews.isNotEmpty()) {
      put(AffectedComponent.Interview, interviews.map { it.interviewUuid }.toSet())
    }
  }

  fun upsert(request: InvestigationRequest) = apply {
    staffInvolved = request.staffInvolved
    evidenceSecured = request.evidenceSecured
    occurrenceReason = request.occurrenceReason
    personsUsualBehaviour = request.personsUsualBehaviour
    personsTrigger = request.personsTrigger
    protectiveFactors = request.protectiveFactors
  }
}
