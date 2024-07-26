package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.ContributoryFactorCreatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateReferral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.actions
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class Referral(
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "record_id", referencedColumnName = "record_id")
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
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "referral_id")
  val id: Long = 0,
) : SimpleAuditable(), PropertyChangeMonitor, Parented {

  override fun parent() = csipRecord

  @PostLoad
  fun resetPropertyChanges() {
    propertyChanges = mutableSetOf()
  }

  @Transient
  override var propertyChanges: MutableSet<PropertyChange> = mutableSetOf()

  @OneToOne(
    mappedBy = "referral",
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  private var decisionAndActions: DecisionAndActions? = null

  fun decisionAndActions() = decisionAndActions

  var incidentDate: LocalDate = incidentDate
    set(value) {
      propertyChanged(::incidentDate, value)
      field = value
    }

  var incidentTime: LocalTime? = incidentTime
    set(value) {
      propertyChanged(::incidentTime, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "incident_type_id")
  var incidentType: ReferenceData = incidentType
    set(value) {
      referenceDataChanged(::incidentType, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "incident_location_id")
  var incidentLocation: ReferenceData = incidentLocation
    set(value) {
      referenceDataChanged(::incidentLocation, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "referer_area_of_work_id")
  var refererAreaOfWork: ReferenceData = refererAreaOfWork
    set(value) {
      referenceDataChanged(::refererAreaOfWork, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "incident_involvement_id")
  var incidentInvolvement: ReferenceData? = incidentInvolvement
    set(value) {
      referenceDataChanged(::incidentInvolvement, value)
      field = value
    }

  @Column(nullable = false, length = 240)
  var referredBy: String = referredBy
    set(value) {
      propertyChanged(::referredBy, value)
      field = value
    }

  var proactiveReferral: Boolean? = proactiveReferral
    set(value) {
      propertyChanged(::proactiveReferral, value)
      field = value
    }

  var staffAssaulted: Boolean? = staffAssaulted
    set(value) {
      propertyChanged(::staffAssaulted, value)
      field = value
    }

  var assaultedStaffName: String? = assaultedStaffName
    set(value) {
      propertyChanged(::assaultedStaffName, value)
      field = value
    }

  var descriptionOfConcern: String? = descriptionOfConcern
    set(value) {
      propertyChanged(::descriptionOfConcern, value)
      field = value
    }

  var knownReasons: String? = knownReasons
    set(value) {
      propertyChanged(::knownReasons, value)
      field = value
    }

  var otherInformation: String? = otherInformation
    set(value) {
      propertyChanged(::otherInformation, value)
      field = value
    }

  @Enumerated(EnumType.STRING)
  var saferCustodyTeamInformed: OptionalYesNoAnswer = saferCustodyTeamInformed
    set(value) {
      propertyChanged(::saferCustodyTeamInformed, value)
      field = value
    }

  var referralComplete: Boolean? = referralComplete
    private set(value) {
      propertyChanged(::referralComplete, value)
      field = value
    }

  var referralCompletedDate: LocalDate? = referralCompletedDate
    private set(value) {
      propertyChanged(::referralCompletedDate, value)
      field = value
    }

  @Column(length = 32)
  var referralCompletedBy: String? = referralCompletedBy
    private set(value) {
      propertyChanged(::referralCompletedBy, value)
      field = value
    }

  @Column(length = 255)
  var referralCompletedByDisplayName: String? = referralCompletedByDisplayName
    private set(value) {
      propertyChanged(::referralCompletedByDisplayName, value)
      field = value
    }

  fun completed(on: LocalDate, by: String, byDisplayName: String) {
    referralCompletedDate = on
    referralCompletedBy = by
    referralCompletedByDisplayName = byDisplayName
    referralComplete = true
  }

  fun uncomplete() {
    referralCompletedDate = null
    referralCompletedBy = null
    referralCompletedByDisplayName = null
    referralComplete = false
  }

  fun createDecisionAndActions(
    context: CsipRequestContext,
    request: CreateDecisionAndActionsRequest,
    referenceProvider: (ReferenceDataType, String) -> ReferenceData,
  ): CsipRecord {
    verifyDecisionDoesNotExist()
    decisionAndActions = DecisionAndActions(
      referral = this,
      outcome = referenceProvider(OUTCOME_TYPE, request.outcomeTypeCode),
      signedOffBy = request.outcomeSignedOffByRoleCode?.let {
        referenceProvider(ReferenceDataType.DECISION_SIGNER_ROLE, it)
      },
      conclusion = request.conclusion,
      recordedBy = request.outcomeRecordedBy,
      recordedByDisplayName = request.outcomeRecordedByDisplayName,
      date = request.outcomeDate,
      nextSteps = request.nextSteps,
      actions = request.actions(),
      actionOther = request.actionOther,
    )

    val description = "Decision and actions added to referral"
    val affectedComponents = setOf(AffectedComponent.DecisionAndActions)
    csipRecord.addAuditEvent(
      context,
      AuditEventAction.CREATED,
      description,
      context.source,
      context.activeCaseLoadId,
      affectedComponents,
    )
    csipRecord.registerEntityEvent(
      CsipUpdatedEvent(
        recordUuid = csipRecord.recordUuid,
        prisonNumber = csipRecord.prisonNumber,
        description = description,
        occurredAt = context.requestAt,
        source = context.source,
        affectedComponents = affectedComponents,
      ),
    )
    return csipRecord
  }

  @OneToOne(
    mappedBy = "referral",
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  private var saferCustodyScreeningOutcome: SaferCustodyScreeningOutcome? = null

  @OneToOne(
    mappedBy = "referral",
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  private var investigation: Investigation? = null

  @OneToMany(
    mappedBy = "referral",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  private var contributoryFactors: MutableList<ContributoryFactor> = mutableListOf()

  fun saferCustodyScreeningOutcome() = saferCustodyScreeningOutcome

  fun investigation() = investigation

  fun contributoryFactors() = contributoryFactors.toList().sortedByDescending { it.id }

  fun createSaferCustodyScreeningOutcome(
    context: CsipRequestContext,
    outcomeType: ReferenceData,
    date: LocalDate,
    reasonForDecision: String,
    source: Source,
    activeCaseLoadId: String?,
  ): CsipRecord {
    verifySaferCustodyScreeningOutcomeDoesNotExist()
    val description = "Safer custody screening outcome added to referral"

    saferCustodyScreeningOutcome = SaferCustodyScreeningOutcome(
      referral = this,
      outcomeType = outcomeType,
      recordedBy = context.username,
      recordedByDisplayName = context.userDisplayName,
      date = date,
      reasonForDecision = reasonForDecision,
    )

    val affectedComponents = setOf(AffectedComponent.SaferCustodyScreeningOutcome)
    csipRecord.addAuditEvent(
      context = context,
      action = AuditEventAction.CREATED,
      description = description,
      source = source,
      activeCaseLoadId = activeCaseLoadId,
      affectedComponents = affectedComponents,
    )
    csipRecord.registerEntityEvent(
      CsipUpdatedEvent(
        recordUuid = csipRecord.recordUuid,
        prisonNumber = csipRecord.prisonNumber,
        description = description,
        occurredAt = context.requestAt,
        source = source,
        affectedComponents = affectedComponents,
      ),
    )
    return csipRecord
  }

  fun createInvestigation(
    context: CsipRequestContext,
    request: CreateInvestigationRequest,
    activeCaseLoadId: String?,
    roleProvider: (Set<String>) -> Map<String, ReferenceData>,
  ): CsipRecord {
    verifyInvestigationDoesNotExist()
    val description = "Investigation with ${request.interviews?.size ?: 0} interviews added to referral"

    val roleCodes = request.interviews?.map { it.intervieweeRoleCode }?.toSet() ?: emptySet()
    val intervieweeRoleMap = if (roleCodes.isEmpty()) mapOf() else roleProvider(roleCodes)

    investigation = Investigation(
      referral = this,
      staffInvolved = request.staffInvolved,
      evidenceSecured = request.evidenceSecured,
      occurrenceReason = request.occurrenceReason,
      personsUsualBehaviour = request.personsUsualBehaviour,
      personsTrigger = request.personsTrigger,
      protectiveFactors = request.protectiveFactors,
    )

    request.interviews?.forEach { interview ->
      investigation!!.addInterview(
        context = context,
        createRequest = interview,
        intervieweeRole = requireNotNull(intervieweeRoleMap[interview.intervieweeRoleCode]),
      )
    }

    val isInterviewAffected = (investigation?.interviews()?.size ?: 0) > 0
    val affectedComponents = buildSet {
      add(AffectedComponent.Investigation)
      if (isInterviewAffected) add(AffectedComponent.Interview)
    }

    csipRecord.addAuditEvent(
      context = context,
      action = AuditEventAction.CREATED,
      description = description,
      source = context.source,
      activeCaseLoadId = activeCaseLoadId,
      affectedComponents = affectedComponents,
    )
    csipRecord.registerEntityEvent(
      CsipUpdatedEvent(
        recordUuid = csipRecord.recordUuid,
        prisonNumber = csipRecord.prisonNumber,
        description = description,
        occurredAt = context.requestAt,
        source = context.source,
        affectedComponents = affectedComponents,
      ),
    )
    return csipRecord
  }

  fun addContributoryFactor(
    createRequest: CreateContributoryFactorRequest,
    factorType: ReferenceData,
    context: CsipRequestContext,
  ) = ContributoryFactor(
    referral = this,
    contributoryFactorType = factorType,
    comment = createRequest.comment,
  ).apply {
    contributoryFactors.add(this)
    csipRecord.registerEntityEvent(
      ContributoryFactorCreatedEvent(
        entityUuid = contributoryFactorUuid,
        recordUuid = csipRecord.recordUuid,
        prisonNumber = csipRecord.prisonNumber,
        description = DomainEventType.CONTRIBUTORY_FACTOR_CREATED.description,
        occurredAt = context.requestAt,
        source = context.source,
      ),
    )
  }

  private fun verifySaferCustodyScreeningOutcomeDoesNotExist() =
    verifyDoesNotExist(saferCustodyScreeningOutcome) {
      ResourceAlreadyExistException("Referral already has a Safer Custody Screening Outcome")
    }

  private fun verifyInvestigationDoesNotExist() =
    verifyDoesNotExist(investigation) { ResourceAlreadyExistException("Referral already has an Investigation") }

  private fun verifyDecisionDoesNotExist() =
    verifyDoesNotExist(decisionAndActions) { ResourceAlreadyExistException("Referral already has a Decision and Actions") }

  fun update(
    context: CsipRequestContext,
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
    if (update.isReferralComplete == true) {
      completed(context.requestAt.toLocalDate(), context.username, context.userDisplayName)
    } else {
      uncomplete()
    }
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
