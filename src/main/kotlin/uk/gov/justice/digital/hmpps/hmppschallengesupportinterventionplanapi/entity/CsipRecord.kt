package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.PlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.PrisonNumberChangeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ReferralRequest
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, DeleteEventListener::class)
class CsipRecord(

  prisonNumber: String,

  @Audited(withModifiedFlag = false)
  @Column(length = 6, updatable = false)
  val prisonCodeWhenRecorded: String? = null,

  logCode: String? = null,

  legacyId: Long? = null,
) : SimpleAuditable(), Identifiable {

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "record_id")
  override val id: UUID = newUuid()

  @Audited(withModifiedFlag = false)
  override var legacyId: Long? = legacyId
    private set

  @Column(nullable = false, length = 10)
  var prisonNumber: String = prisonNumber
    private set

  @Column(length = 10)
  var logCode: String? = logCode
    private set

  @NotAudited
  @OneToOne(mappedBy = "csipRecord", cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE])
  var referral: Referral? = null
    private set

  @NotAudited
  @OneToOne(mappedBy = "csipRecord", cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE])
  var plan: Plan? = null
    private set

  @NotAudited
  @Enumerated(EnumType.STRING)
  @Column(insertable = false, updatable = false)
  var status: CsipStatus = CsipStatus.UNKNOWN
    private set

  fun createReferral(
    request: ReferralRequest,
    csipRequestContext: CsipRequestContext,
    rdSupplier: (ReferenceDataType, String) -> ReferenceData,
  ): Referral = let {
    referral = referral(csipRequestContext, request, rdSupplier)
    referral!!
  }

  fun update(request: CsipRequest): CsipRecord = apply {
    logCode = request.logCode
    if (request is PrisonNumberChangeRequest) {
      prisonNumber = request.prisonNumber
    }
    if (request is LegacyIdAware) {
      legacyId = request.legacyId
    }
  }

  fun updateWithReferral(
    request: CsipRequest,
    referenceProvider: (ReferenceDataType, String) -> ReferenceData,
  ): CsipRecord = apply {
    val referral = requireNotNull(referral)
    logCode = request.logCode
    request.referral?.also { referral.update(it, referenceProvider) }
  }

  fun upsertPlan(request: PlanRequest) = let {
    if (plan == null) {
      plan = Plan(this, request.caseManager, request.reasonForPlan, request.firstCaseReviewDate)
    } else {
      plan!!.update(request)
    }
    plan!!
  }

  fun components(): Set<CsipComponent> = buildSet {
    add(CsipComponent.RECORD)
    referral?.also { addAll(it.components()) }
    plan?.also { addAll(it.components()) }
  }

  private fun referral(
    csipRequestContext: CsipRequestContext,
    request: ReferralRequest,
    rdSupplier: (ReferenceDataType, String) -> ReferenceData,
  ) = Referral(
    csipRecord = this,
    referralDate = csipRequestContext.requestAt.toLocalDate(),
    incidentDate = request.incidentDate,
    incidentTime = request.incidentTime,
    referredBy = request.referredBy,
    proactiveReferral = request.isProactiveReferral,
    staffAssaulted = request.isStaffAssaulted,
    assaultedStaffName = request.assaultedStaffName,
    descriptionOfConcern = request.descriptionOfConcern,
    knownReasons = request.knownReasons,
    otherInformation = request.otherInformation,
    saferCustodyTeamInformed = request.isSaferCustodyTeamInformed,
    incidentType = rdSupplier(INCIDENT_TYPE, request.incidentTypeCode),
    incidentLocation = rdSupplier(INCIDENT_LOCATION, request.incidentLocationCode),
    refererAreaOfWork = rdSupplier(ReferenceDataType.AREA_OF_WORK, request.refererAreaCode),
    incidentInvolvement = request.incidentInvolvementCode?.let { rdSupplier(INCIDENT_INVOLVEMENT, it) },
    referralComplete = request.isReferralComplete,
    referralCompletedDate = request.completedDate,
    referralCompletedBy = request.completedBy,
    referralCompletedByDisplayName = request.completedByDisplayName,
  )

  companion object {
    val PRISON_NUMBER: String = CsipRecord::prisonNumber.name
    val LOG_CODE: String = CsipRecord::logCode.name
    val CREATED_AT: String = CsipRecord::createdAt.name
  }
}
