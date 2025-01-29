package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral

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
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.SimpleVersion
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.ifAppended
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.newUuid
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.InterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(CsipChangedListener::class)
@BatchSize(size = 20)
class Interview(
  @Audited(withModifiedFlag = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "investigation_id")
  val investigation: Investigation,

  interviewee: String,
  interviewDate: LocalDate,
  intervieweeRole: ReferenceData,
  interviewText: String?,

  legacyId: Long? = null,
) : SimpleVersion(), Identifiable, CsipAware {
  override fun csipRecord() = investigation.referral.csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "interview_id")
  override val id: UUID = newUuid()

  @Audited(withModifiedFlag = false)
  override var legacyId: Long? = legacyId
    private set

  @Column(length = 100)
  var interviewee: String = interviewee
    private set

  var interviewDate: LocalDate = interviewDate
    private set

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "interviewee_role_id")
  var intervieweeRole: ReferenceData = intervieweeRole
    private set

  var interviewText: String? = interviewText
    private set

  fun update(request: InterviewRequest, rdSupplier: (ReferenceDataType, String) -> ReferenceData) = apply {
    interviewee = request.interviewee
    interviewDate = request.interviewDate
    intervieweeRole = rdSupplier(ReferenceDataType.INTERVIEWEE_ROLE, request.intervieweeRoleCode)
    ::interviewText.ifAppended(request.interviewText)
    if (request is LegacyIdAware) {
      legacyId = request.legacyId
    }
  }
}

interface InterviewRepository : JpaRepository<Interview, UUID>

fun InterviewRepository.getInterview(id: UUID) =
  findByIdOrNull(id) ?: throw NotFoundException("Interview", id.toString())

fun Interview.toModel() =
  uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.Interview(
    interviewUuid = id,
    interviewee = interviewee,
    interviewDate = interviewDate,
    intervieweeRole = intervieweeRole.toReferenceDataModel(),
    interviewText = interviewText,
  )
