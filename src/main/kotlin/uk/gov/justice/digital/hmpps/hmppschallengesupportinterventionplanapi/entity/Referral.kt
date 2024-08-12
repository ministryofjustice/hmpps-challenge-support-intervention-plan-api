package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.InvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateReferral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertDecisionAndActionsRequest
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class)
class Referral(
  @Audited(withModifiedFlag = false)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "referral_id")
  val csipRecord: CsipRecord,

  @Column(nullable = false) val referralDate: LocalDate,

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
  referralComplete: Boolean? = null,
  referralCompletedBy: String? = null,
  referralCompletedByDisplayName: String? = null,

  referralCompletedDate: LocalDate? = null,
  incidentType: ReferenceData,
  incidentLocation: ReferenceData,

  refererAreaOfWork: ReferenceData,
  incidentInvolvement: ReferenceData?,

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "referral_id")
  val id: Long = 0,
) : SimpleAuditable(), CsipAware {

  override fun csipRecord() = csipRecord

  @NotAudited
  @OneToOne(mappedBy = "referral", cascade = [CascadeType.ALL])
  var saferCustodyScreeningOutcome: SaferCustodyScreeningOutcome? = null
    private set

  @NotAudited
  @OneToOne(mappedBy = "referral", cascade = [CascadeType.ALL])
  var decisionAndActions: DecisionAndActions? = null
    private set

  @NotAudited
  @OneToOne(mappedBy = "referral", cascade = [CascadeType.ALL])
  var investigation: Investigation? = null
    private set

  @NotAudited
  @OneToMany(mappedBy = "referral", cascade = [CascadeType.ALL])
  private var contributoryFactors: MutableList<ContributoryFactor> = mutableListOf()

  fun contributoryFactors() = contributoryFactors.toList().sortedByDescending { it.id }

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

  var referralComplete: Boolean? = referralComplete
    private set

  var referralCompletedDate: LocalDate? = referralCompletedDate
    private set

  @Column(length = 32)
  var referralCompletedBy: String? = referralCompletedBy
    private set

  @Column(length = 255)
  var referralCompletedByDisplayName: String? = referralCompletedByDisplayName
    private set

  fun upsertDecisionAndActions(
    request: UpsertDecisionAndActionsRequest,
    referenceProvider: (ReferenceDataType, String) -> ReferenceData,
  ): CsipRecord {
    val isNew = decisionAndActions == null
    val outcome = referenceProvider(OUTCOME_TYPE, request.outcomeTypeCode)
    val signedOffBy = request.signedOffByRoleCode?.let {
      referenceProvider(ReferenceDataType.DECISION_SIGNER_ROLE, it)
    }
    if (isNew) {
      decisionAndActions = DecisionAndActions(this, outcome)
    }
    decisionAndActions!!.upsert(request, outcome, signedOffBy)
    return csipRecord
  }

  fun createSaferCustodyScreeningOutcome(
    request: CreateSaferCustodyScreeningOutcomeRequest,
    outcomeType: ReferenceData,
  ): CsipRecord {
    verifySaferCustodyScreeningOutcomeDoesNotExist()

    saferCustodyScreeningOutcome = SaferCustodyScreeningOutcome(
      referral = this,
      outcome = outcomeType,
      recordedBy = request.recordedBy,
      recordedByDisplayName = request.recordedByDisplayName,
      date = request.date,
      reasonForDecision = request.reasonForDecision,
    )
    return csipRecord
  }

  fun upsertInvestigation(
    request: InvestigationRequest,
  ): CsipRecord {
    investigation = investigation ?: Investigation(this)
    investigation!!.upsert(request)
    return csipRecord
  }

  fun addContributoryFactor(
    createRequest: CreateContributoryFactorRequest,
    factorType: ReferenceData,
  ) = ContributoryFactor(
    referral = this,
    contributoryFactorType = factorType,
    comment = createRequest.comment,
  ).apply {
    contributoryFactors.add(this)
  }

  private fun verifySaferCustodyScreeningOutcomeDoesNotExist() =
    verifyDoesNotExist(saferCustodyScreeningOutcome) {
      ResourceAlreadyExistException("Referral already has a Safer Custody Screening Outcome")
    }

  fun update(
    update: UpdateReferral,
    referenceProvider: (ReferenceDataType, String) -> ReferenceData,
  ) {
    incidentType = updateReferenceData(incidentType, referenceProvider, INCIDENT_TYPE, update.incidentTypeCode)
    incidentLocation =
      updateReferenceData(incidentLocation, referenceProvider, INCIDENT_LOCATION, update.incidentLocationCode)
    refererAreaOfWork = updateReferenceData(refererAreaOfWork, referenceProvider, AREA_OF_WORK, update.refererAreaCode)
    incidentInvolvement = if (update.incidentInvolvementCode == null) {
      null
    } else {
      referenceProvider(INCIDENT_INVOLVEMENT, update.incidentInvolvementCode)
    }

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
    referralComplete = update.isReferralComplete
    referralCompletedDate = update.completedDate
    referralCompletedBy = update.completedBy
    referralCompletedByDisplayName = update.completedByDisplayName
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

  fun components(): Set<CsipComponent> = buildSet {
    add(CsipComponent.REFERRAL)
    saferCustodyScreeningOutcome?.also { add(CsipComponent.SAFER_CUSTODY_SCREENING_OUTCOME) }
    decisionAndActions?.also { add(CsipComponent.DECISION_AND_ACTIONS) }
    investigation?.also { addAll(it.components()) }
    if (contributoryFactors.isNotEmpty()) {
      add(CsipComponent.CONTRIBUTORY_FACTOR)
    }
  }
}
