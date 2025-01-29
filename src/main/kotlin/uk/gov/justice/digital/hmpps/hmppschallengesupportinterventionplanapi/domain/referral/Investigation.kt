package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral

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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.SimpleVersion
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.ifAppended
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.InterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.InvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(CsipChangedListener::class)
class Investigation(
  @Audited(withModifiedFlag = false)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "investigation_id")
  val referral: Referral,
) : SimpleVersion(), CsipAware {
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
    request: InterviewRequest,
    rdSupplier: (ReferenceDataType, String) -> ReferenceData,
  ) = Interview(
    investigation = this,
    interviewee = request.interviewee,
    interviewDate = request.interviewDate,
    intervieweeRole = rdSupplier(ReferenceDataType.INTERVIEWEE_ROLE, request.intervieweeRoleCode),
    interviewText = request.interviewText,
    legacyId = if (request is LegacyIdAware) request.legacyId else null,
  ).apply {
    interviews.add(this)
  }

  fun update(request: InvestigationRequest) = apply {
    ::staffInvolved.ifAppended(request.staffInvolved)
    ::evidenceSecured.ifAppended(request.evidenceSecured)
    ::occurrenceReason.ifAppended(request.occurrenceReason)
    ::personsUsualBehaviour.ifAppended(request.personsUsualBehaviour)
    ::personsTrigger.ifAppended(request.personsTrigger)
    ::protectiveFactors.ifAppended(request.protectiveFactors)
  }
}

fun Investigation.toModel() =
  uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.Investigation(
    staffInvolved = staffInvolved,
    evidenceSecured = evidenceSecured,
    occurrenceReason = occurrenceReason,
    personsUsualBehaviour = personsUsualBehaviour,
    personsTrigger = personsTrigger,
    protectiveFactors = protectiveFactors,
    interviews = interviews().map { it.toModel() },
  )
