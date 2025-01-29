package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.SimpleVersion
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.PlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CompletableRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ReferralDateRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.asCompletable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(CsipChangedListener::class)
class CsipRecord(

  personSummary: PersonSummary,

  @Audited(withModifiedFlag = false)
  @Column(length = 6, updatable = false)
  val prisonCodeWhenRecorded: String? = null,

  logCode: String? = null,

  legacyId: Long? = null,
) : SimpleVersion(),
  Identifiable {

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "record_id")
  override val id: UUID = newUuid()

  @Audited(withModifiedFlag = false)
  override var legacyId: Long? = legacyId
    private set

  @Audited(withModifiedFlag = true, modifiedColumnName = "prison_number_modified")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_number")
  var personSummary: PersonSummary = personSummary
    private set

  @NotAudited
  @Column(name = "prison_number", insertable = false, updatable = false)
  var prisonNumber: String = personSummary.prisonNumber
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
  @OneToOne
  @JoinColumn(name = "status_id", insertable = false, updatable = false)
  var status: ReferenceData? = null
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
    if (request is LegacyIdAware) {
      legacyId = request.legacyId
    }
  }

  fun updateWithReferral(
    request: CsipRequest,
    referenceProvider: (ReferenceDataType, String) -> ReferenceData,
  ): CsipRecord = apply {
    val referral = verifyExists(referral) { MissingReferralException(id) }
    logCode = request.logCode
    request.referral?.also { referral.update(it, referenceProvider) }
  }

  fun createPlan(request: PlanRequest) = let {
    verifyDoesNotExist(plan) { ResourceAlreadyExistException("CSIP record already has a plan") }
    plan = Plan(this, request.caseManager, request.reasonForPlan, request.nextCaseReviewDate)
    plan!!
  }

  fun moveTo(personSummary: PersonSummary) = apply {
    this.personSummary = personSummary
    this.prisonNumber = personSummary.prisonNumber
  }

  private fun referral(
    context: CsipRequestContext,
    request: ReferralRequest,
    rdSupplier: (ReferenceDataType, String) -> ReferenceData,
  ): Referral {
    val completable = if (request is CompletableRequest) {
      request
    } else {
      context.asCompletable(request.isReferralComplete)
    }

    return Referral(
      csipRecord = this,
      referralDate = if (request is ReferralDateRequest) request.referralDate else context.requestAt.toLocalDate(),
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
    ).apply { complete(completable) }
  }

  companion object {
    val PRISON_NUMBER: String = CsipRecord::prisonNumber.name
  }
}

fun CsipRecord.toModel() = uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord(
  recordUuid = id,
  prisonNumber = prisonNumber,
  prisonCodeWhenRecorded = prisonCodeWhenRecorded,
  logCode = logCode,
  referral = referral!!.toModel(),
  plan = plan?.toModel(),
  status = requireNotNull(status).toReferenceDataModel(),
)
