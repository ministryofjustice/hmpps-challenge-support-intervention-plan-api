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
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
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
) : SimpleAuditable(), CsipAware {
  override fun csipRecord() = referral.csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "investigation_id")
  val id: UUID = referral.id

  var staffInvolved: String? = null
    private set

  var evidenceSecured: String? = null
    private set

  var occurrenceReason: String? = null
    private set

  var personsUsualBehaviour: String? = null
    private set

  var personsTrigger: String? = null
    private set

  var protectiveFactors: String? = null
    private set

  @NotAudited
  @OneToMany(mappedBy = "investigation", cascade = [CascadeType.ALL])
  private val interviews: MutableList<Interview> = mutableListOf()

  fun interviews() = interviews.toList().sortedByDescending { it.id }

  fun addInterview(
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
  }

  fun upsert(request: InvestigationRequest) = apply {
    staffInvolved = request.staffInvolved
    evidenceSecured = request.evidenceSecured
    occurrenceReason = request.occurrenceReason
    personsUsualBehaviour = request.personsUsualBehaviour
    personsTrigger = request.personsTrigger
    protectiveFactors = request.protectiveFactors
  }

  fun components(): Set<CsipComponent> = buildSet {
    add(CsipComponent.INVESTIGATION)
    if (interviews.isNotEmpty()) {
      add(CsipComponent.INTERVIEW)
    }
  }
}
