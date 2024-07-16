package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.InterviewCreatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import java.time.LocalDateTime

@Entity
@Table
class Investigation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "investigation_id")
  val investigationId: Long = 0,

  @OneToOne(fetch = FetchType.LAZY) @JoinColumn(
    name = "referral_id",
    referencedColumnName = "referral_id",
  ) val referral: Referral,

  var staffInvolved: String?,
  var evidenceSecured: String?,
  var occurrenceReason: String?,
  var personsUsualBehaviour: String?,
  var personsTrigger: String?,
  var protectiveFactors: String?,
) : AbstractAggregateRoot<Investigation>() {
  @OneToMany(
    mappedBy = "investigation",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  private var interviews: MutableList<Interview> = mutableListOf()

  fun interviews() = interviews.toList().sortedByDescending { it.interviewId }

  fun addInterview(
    createRequest: CreateInterviewRequest,
    intervieweeRole: ReferenceData,
    actionedAt: LocalDateTime = LocalDateTime.now(),
    actionedBy: String,
    actionedByDisplayName: String,
    source: Source,
  ) = Interview(
    investigation = this,
    interviewee = createRequest.interviewee,
    interviewDate = createRequest.interviewDate,
    intervieweeRole = intervieweeRole,
    interviewText = createRequest.interviewText,
    createdAt = actionedAt,
    createdBy = actionedBy,
    createdByDisplayName = actionedByDisplayName,
  ).apply {
    interviews.add(this)
    referral.csipRecord.registerEntityEvent(
      InterviewCreatedEvent(
        entityUuid = interviewUuid,
        recordUuid = referral.csipRecord.recordUuid,
        prisonNumber = referral.csipRecord.prisonNumber,
        description = DomainEventType.INTERVIEW_CREATED.description,
        occurredAt = actionedAt,
        source = source,
        updatedBy = actionedBy,
      ),
    )
  }
}
