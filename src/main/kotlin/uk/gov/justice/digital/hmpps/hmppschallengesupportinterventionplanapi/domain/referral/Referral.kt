package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.AuditedEntityListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.SimpleAuditable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.DECISION_OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.DECISION_SIGNER_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.SCREENING_OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CompletableRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.DecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.InvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ReferralDateRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.asCompletable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, CsipChangedListener::class)
class Referral(
  @Audited(withModifiedFlag = false)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "referral_id")
  val csipRecord: CsipRecord,

  referralDate: LocalDate,

  incidentDate: LocalDate,
  incidentTime: LocalTime? = null,
  referredBy: String,

  proactiveReferral: Boolean? = null,

  staffAssaulted: Boolean? = null,
  assaultedStaffName: String? = null,
  descriptionOfConcern: String?,

  knownReasons: String?,

  otherInformation: String? = null,
  saferCustodyTeamInformed: OptionalYesNoAnswer,
  incidentType: ReferenceData,
  incidentLocation: ReferenceData,
  refererAreaOfWork: ReferenceData,

  incidentInvolvement: ReferenceData?,
) : SimpleAuditable(), CsipAware {
  override fun csipRecord() = csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "referral_id")
  val id: UUID = csipRecord.id

  @NotAudited
  @OneToMany(mappedBy = "referral", cascade = [CascadeType.ALL])
  private var contributoryFactors: MutableList<ContributoryFactor> = mutableListOf()

  @NotAudited
  @OneToOne(mappedBy = "referral", cascade = [CascadeType.ALL])
  var saferCustodyScreeningOutcome: SaferCustodyScreeningOutcome? = null
    private set

  @NotAudited
  @OneToOne(mappedBy = "referral", cascade = [CascadeType.ALL])
  var investigation: Investigation? = null
    private set

  @NotAudited
  @OneToOne(mappedBy = "referral", cascade = [CascadeType.ALL])
  var decisionAndActions: DecisionAndActions? = null
    private set

  fun contributoryFactors() = contributoryFactors.toList().sortedByDescending { it.id }

  @Column(nullable = false)
  var referralDate: LocalDate = referralDate
    private set

  var incidentDate: LocalDate = incidentDate
    private set

  var incidentTime: LocalTime? = incidentTime
    private set

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "incident_type_id")
  var incidentType: ReferenceData = incidentType
    private set

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "incident_location_id")
  var incidentLocation: ReferenceData = incidentLocation
    private set

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "referer_area_of_work_id")
  var refererAreaOfWork: ReferenceData = refererAreaOfWork
    private set

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "incident_involvement_id")
  var incidentInvolvement: ReferenceData? = incidentInvolvement
    private set

  @Column(nullable = false, length = 240)
  var referredBy: String = referredBy
    private set

  var proactiveReferral: Boolean? = proactiveReferral
    private set

  var staffAssaulted: Boolean? = staffAssaulted
    private set

  var assaultedStaffName: String? = assaultedStaffName
    private set

  var descriptionOfConcern: String? = descriptionOfConcern
    private set

  var knownReasons: String? = knownReasons
    private set

  var otherInformation: String? = otherInformation
    private set

  @Enumerated(EnumType.STRING)
  var saferCustodyTeamInformed: OptionalYesNoAnswer = saferCustodyTeamInformed
    private set

  var referralComplete: Boolean? = null
    private set

  var referralCompletedDate: LocalDate? = null
    private set

  @Column(length = 32)
  var referralCompletedBy: String? = null
    private set

  @Column(length = 255)
  var referralCompletedByDisplayName: String? = null
    private set

  fun update(
    update: ReferralRequest,
    rdSupplier: (ReferenceDataType, String) -> ReferenceData,
  ): Referral = apply {
    incidentType = updateReferenceData(incidentType, rdSupplier, INCIDENT_TYPE, update.incidentTypeCode)
    incidentLocation =
      updateReferenceData(incidentLocation, rdSupplier, INCIDENT_LOCATION, update.incidentLocationCode)
    refererAreaOfWork = updateReferenceData(refererAreaOfWork, rdSupplier, AREA_OF_WORK, update.refererAreaCode)
    incidentInvolvement = update.incidentInvolvementCode?.let { rdSupplier(INCIDENT_INVOLVEMENT, it) }

    incidentDate = update.incidentDate
    incidentTime = update.incidentTime
    referredBy = update.referredBy
    proactiveReferral = update.isProactiveReferral
    staffAssaulted = update.isStaffAssaulted
    assaultedStaffName = update.assaultedStaffName
    descriptionOfConcern = update.descriptionOfConcern
    knownReasons = update.knownReasons
    otherInformation = update.otherInformation
    saferCustodyTeamInformed = update.isSaferCustodyTeamInformed

    if (update is CompletableRequest) {
      complete(update)
    } else if (update.isReferralComplete != referralComplete) {
      complete(csipRequestContext().asCompletable(update.isReferralComplete))
    }

    if (update is ReferralDateRequest) {
      referralDate = update.referralDate
    }
  }

  fun complete(request: CompletableRequest): Referral = apply {
    referralCompletedDate = request.completedDate
    referralCompletedBy = request.completedBy
    referralCompletedByDisplayName = request.completedByDisplayName
    referralComplete = request.completed
  }

  fun addContributoryFactor(
    request: ContributoryFactorRequest,
    rdSupplier: (ReferenceDataType, String) -> ReferenceData,
  ) = ContributoryFactor(
    referral = this,
    contributoryFactorType = rdSupplier(CONTRIBUTORY_FACTOR_TYPE, request.factorTypeCode),
    comment = request.comment,
    legacyId = if (request is LegacyIdAware) request.legacyId else null,
  ).apply {
    contributoryFactors.add(this)
  }

  fun createSaferCustodyScreeningOutcome(
    request: ScreeningOutcomeRequest,
    rdSupplier: (ReferenceDataType, String) -> ReferenceData,
  ): SaferCustodyScreeningOutcome {
    verifyDoesNotExist(saferCustodyScreeningOutcome) {
      ResourceAlreadyExistException("Referral already has a Safer Custody Screening Outcome")
    }
    saferCustodyScreeningOutcome = SaferCustodyScreeningOutcome(
      referral = this,
      outcome = rdSupplier(SCREENING_OUTCOME_TYPE, request.outcomeTypeCode),
      date = request.date,
      recordedBy = request.recordedBy,
      recordedByDisplayName = request.recordedByDisplayName,
      reasonForDecision = request.reasonForDecision,
    )
    return saferCustodyScreeningOutcome!!
  }

  fun createInvestigation(
    request: InvestigationRequest,
  ): Investigation {
    verifyDoesNotExist(investigation) {
      ResourceAlreadyExistException("Referral already has an investigation")
    }
    investigation = Investigation(this).update(request)
    return investigation!!
  }

  fun upsertDecisionAndActions(
    request: DecisionAndActionsRequest,
    rdSupplier: (ReferenceDataType, String) -> ReferenceData,
  ): DecisionAndActions {
    val isNew = decisionAndActions == null
    val outcome = request.outcomeTypeCode?.let { rdSupplier(DECISION_OUTCOME_TYPE, it) }
    val signedOffBy = request.signedOffByRoleCode?.let { rdSupplier(DECISION_SIGNER_ROLE, it) }
    if (isNew) {
      decisionAndActions = DecisionAndActions(this, outcome, signedOffBy)
    }
    decisionAndActions!!.upsert(request, outcome, signedOffBy)
    return decisionAndActions!!
  }

  private fun updateReferenceData(
    existing: ReferenceData,
    referenceProvider: (ReferenceDataType, String) -> ReferenceData,
    type: ReferenceDataType,
    newCode: String,
  ): ReferenceData =
    if (newCode != existing.code) {
      referenceProvider(type, newCode)
    } else {
      existing
    }
}

fun Referral.toModel() = uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.Referral(
  referralDate = referralDate,
  incidentDate = incidentDate,
  incidentTime = incidentTime,
  incidentType = incidentType.toReferenceDataModel(),
  incidentLocation = incidentLocation.toReferenceDataModel(),
  incidentInvolvement = incidentInvolvement?.toReferenceDataModel(),
  refererArea = refererAreaOfWork.toReferenceDataModel(),
  referredBy = referredBy,
  otherInformation = otherInformation,
  knownReasons = knownReasons,
  descriptionOfConcern = descriptionOfConcern,
  assaultedStaffName = assaultedStaffName,
  isProactiveReferral = proactiveReferral,
  isStaffAssaulted = staffAssaulted,
  isReferralComplete = referralComplete,
  referralCompletedDate = referralCompletedDate,
  referralCompletedBy = referralCompletedBy,
  referralCompletedByDisplayName = referralCompletedByDisplayName,
  isSaferCustodyTeamInformed = saferCustodyTeamInformed,
  saferCustodyScreeningOutcome = saferCustodyScreeningOutcome?.toModel(),
  decisionAndActions = decisionAndActions?.toModel(),
  investigation = investigation?.toModel(),
  contributoryFactors = contributoryFactors().map { it.toModel() },
)
