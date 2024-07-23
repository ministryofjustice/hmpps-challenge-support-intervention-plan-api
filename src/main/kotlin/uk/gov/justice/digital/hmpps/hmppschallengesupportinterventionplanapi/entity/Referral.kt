package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateReferral
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table
class Referral(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "referral_id")
  val referralId: Long = 0,

  @OneToOne(fetch = FetchType.LAZY) @JoinColumn(
    name = "record_id",
    referencedColumnName = "record_id",
  ) val csipRecord: CsipRecord,

  incidentDate: LocalDate,
  incidentTime: LocalTime? = null,
  referredBy: String,

  @Column(nullable = false) val referralDate: LocalDate,

  proactiveReferral: Boolean? = null,
  staffAssaulted: Boolean? = null,
  assaultedStaffName: String? = null,

  @Column val releaseDate: LocalDate? = null,

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
) {
  @OneToOne(
    mappedBy = "referral",
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  private var decisionAndActions: DecisionAndActions? = null

  fun decisionAndActions() = decisionAndActions

  @PostLoad
  internal fun clearPropertyChanges() {
    propertyChanges = mutableSetOf()
  }

  @field:Transient
  private var propertyChanges: MutableSet<PropertyChange> = mutableSetOf()

  internal fun propertyChanges(): Set<PropertyChange> = propertyChanges.toSet()

  private fun listenForChanges(name: String, old: Any?, new: Any?) {
    if (old != new) {
      propertyChanges.add(PropertyChange(name, old, new))
    }
  }

  private fun listenForRdChanges(name: String, old: ReferenceData?, new: ReferenceData?) {
    if (old?.code != new?.code) {
      propertyChanges.add(PropertyChange(name, old?.code, new?.code))
    }
  }

  var incidentDate: LocalDate = incidentDate
    set(value) {
      listenForChanges("incidentDate", field, value)
      field = value
    }

  var incidentTime: LocalTime? = incidentTime
    set(value) {
      listenForChanges("incidentTime", field, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "incident_type_id", referencedColumnName = "reference_data_id")
  var incidentType: ReferenceData = incidentType
    set(value) {
      listenForRdChanges("incidentType", field, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "incident_location_id", referencedColumnName = "reference_data_id")
  var incidentLocation: ReferenceData = incidentLocation
    set(value) {
      listenForRdChanges("incidentLocation", field, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "referer_area_of_work_id", referencedColumnName = "reference_data_id")
  var refererAreaOfWork: ReferenceData = refererAreaOfWork
    set(value) {
      listenForRdChanges("refererAreaOfWork", field, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "incident_involvement_id", referencedColumnName = "reference_Data_id")
  var incidentInvolvement: ReferenceData? = incidentInvolvement
    set(value) {
      listenForRdChanges("incidentInvolvement", field, value)
      field = value
    }

  @Column(nullable = false, length = 240)
  var referredBy: String = referredBy
    set(value) {
      listenForChanges("referredBy", field, value)
      field = value
    }

  var proactiveReferral: Boolean? = proactiveReferral
    set(value) {
      listenForChanges("proactiveReferral", field, value)
      field = value
    }

  var staffAssaulted: Boolean? = staffAssaulted
    set(value) {
      listenForChanges("staffAssaulted", field, value)
      field = value
    }

  var assaultedStaffName: String? = assaultedStaffName
    set(value) {
      listenForChanges("assaultedStaffName", field, value)
      field = value
    }

  var descriptionOfConcern: String? = descriptionOfConcern
    set(value) {
      listenForChanges("descriptionOfConcern", field, value)
      field = value
    }

  var knownReasons: String? = knownReasons
    set(value) {
      listenForChanges("knownReasons", field, value)
      field = value
    }

  var otherInformation: String? = otherInformation
    set(value) {
      listenForChanges("otherInformation", field, value)
      field = value
    }

  @Enumerated(EnumType.STRING)
  var saferCustodyTeamInformed: OptionalYesNoAnswer = saferCustodyTeamInformed
    set(value) {
      listenForChanges("saferCustodyTeamInformed", field, value)
      field = value
    }

  var referralComplete: Boolean? = referralComplete
    private set(value) {
      listenForChanges("referralComplete", field, value)
      field = value
    }

  var referralCompletedDate: LocalDate? = referralCompletedDate
    private set(value) {
      listenForChanges("referralCompletedDate", field, value)
      field = value
    }

  @Column(length = 32)
  var referralCompletedBy: String? = referralCompletedBy
    private set(value) {
      listenForChanges("referralCompletedBy", field, value)
      field = value
    }

  @Column(length = 255)
  var referralCompletedByDisplayName: String? = referralCompletedByDisplayName
    private set(value) {
      listenForChanges("referralCompletedByDisplayName", field, value)
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
    decisionOutcome: ReferenceData,
    decisionOutcomeSignedOffBy: ReferenceData?,
    decisionConclusion: String?,
    decisionOutcomeRecordedBy: String,
    decisionOutcomeRecordedByDisplayName: String,
    decisionOutcomeDate: LocalDate?,
    nextSteps: String?,
    actionOther: String?,
    actionedAt: LocalDateTime,
    source: Source,
    activeCaseLoadId: String?,
    actionOpenCsipAlert: Boolean,
    actionNonAssociationsUpdated: Boolean,
    actionObservationBook: Boolean,
    actionUnitOrCellMove: Boolean,
    actionCsraOrRsraReview: Boolean,
    actionServiceReferral: Boolean,
    actionSimReferral: Boolean,
    description: String = "Decision and actions added to referral",
  ): CsipRecord {
    verifyDecisionDoesNotExist()

    decisionAndActions = DecisionAndActions(
      referral = this,
      decisionOutcome = decisionOutcome,
      decisionOutcomeSignedOffBy = decisionOutcomeSignedOffBy,
      decisionConclusion = decisionConclusion,
      decisionOutcomeRecordedBy = decisionOutcomeRecordedBy,
      decisionOutcomeRecordedByDisplayName = decisionOutcomeRecordedByDisplayName,
      decisionOutcomeDate = decisionOutcomeDate,
      nextSteps = nextSteps,
      actionOpenCsipAlert = actionOpenCsipAlert,
      actionNonAssociationsUpdated = actionNonAssociationsUpdated,
      actionObservationBook = actionObservationBook,
      actionUnitOrCellMove = actionUnitOrCellMove,
      actionCsraOrRsraReview = actionCsraOrRsraReview,
      actionServiceReferral = actionServiceReferral,
      actionSimReferral = actionSimReferral,
      actionOther = actionOther,
    )

    val affectedComponents = setOf(AffectedComponent.DecisionAndActions)
    with(csipRecord) {
      addAuditEvent(
        action = AuditEventAction.CREATED,
        description,
        actionedAt,
        actionedBy = decisionOutcomeRecordedBy,
        actionedByCapturedName = decisionOutcomeRecordedByDisplayName,
        source,
        activeCaseLoadId,
        affectedComponents = affectedComponents,
      )
      csipRecord.registerEntityEvent(
        CsipUpdatedEvent(
          recordUuid = csipRecord.recordUuid,
          prisonNumber = csipRecord.prisonNumber,
          description = description,
          occurredAt = actionedAt,
          source = source,
          updatedBy = decisionOutcomeRecordedBy,
          affectedComponents = affectedComponents,
        ),
      )
    }
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

  fun contributoryFactors() = contributoryFactors.toList().sortedByDescending { it.contributoryFactorId }

  fun createSaferCustodyScreeningOutcome(
    outcomeType: ReferenceData,
    date: LocalDate,
    reasonForDecision: String,
    actionedAt: LocalDateTime = LocalDateTime.now(),
    actionedBy: String,
    actionedByDisplayName: String,
    source: Source,
    activeCaseLoadId: String?,
  ): CsipRecord {
    verifySaferCustodyScreeningOutcomeDoesNotExist()
    val description = "Safer custody screening outcome added to referral"

    saferCustodyScreeningOutcome = SaferCustodyScreeningOutcome(
      referral = this,
      outcomeType = outcomeType,
      recordedBy = actionedBy,
      recordedByDisplayName = actionedByDisplayName,
      date = date,
      reasonForDecision = reasonForDecision,
    )

    val affectedComponents = setOf(AffectedComponent.SaferCustodyScreeningOutcome)
    with(csipRecord) {
      addAuditEvent(
        action = AuditEventAction.CREATED,
        description = description,
        actionedAt = actionedAt,
        actionedBy = actionedBy,
        actionedByCapturedName = actionedByDisplayName,
        source = source,
        activeCaseLoadId = activeCaseLoadId,
        affectedComponents = affectedComponents,
      )
      csipRecord.registerEntityEvent(
        CsipUpdatedEvent(
          recordUuid = csipRecord.recordUuid,
          prisonNumber = csipRecord.prisonNumber,
          description = description,
          occurredAt = actionedAt,
          source = source,
          updatedBy = actionedBy,
          affectedComponents = affectedComponents,
        ),
      )
    }

    return csipRecord
  }

  fun createInvestigation(
    createRequest: CreateInvestigationRequest,
    intervieweeRoleMap: Map<String, ReferenceData>,
    actionedAt: LocalDateTime = LocalDateTime.now(),
    actionedBy: String,
    actionedByDisplayName: String,
    activeCaseLoadId: String?,
    source: Source,
  ): CsipRecord {
    verifyInvestigationDoesNotExist()
    val description = "Investigation with ${createRequest.interviews?.size ?: 0} interviews added to referral"

    investigation = Investigation(
      referral = this,
      staffInvolved = createRequest.staffInvolved,
      evidenceSecured = createRequest.evidenceSecured,
      occurrenceReason = createRequest.occurrenceReason,
      personsUsualBehaviour = createRequest.personsUsualBehaviour,
      personsTrigger = createRequest.personsTrigger,
      protectiveFactors = createRequest.protectiveFactors,
    )

    createRequest.interviews?.forEach { interview ->
      investigation!!.addInterview(
        createRequest = interview,
        intervieweeRole = intervieweeRoleMap[interview.intervieweeRoleCode]!!,
        actionedAt = actionedAt,
        actionedBy = actionedBy,
        actionedByDisplayName = actionedByDisplayName,
        source = source,
      )
    }

    val isInterviewAffected = (investigation?.interviews()?.size ?: 0) > 0
    val affectedComponents = buildSet {
      add(AffectedComponent.Investigation)
      if (isInterviewAffected) add(AffectedComponent.Interview)
    }

    with(csipRecord) {
      addAuditEvent(
        action = AuditEventAction.CREATED,
        description = description,
        actionedAt = actionedAt,
        actionedBy = actionedBy,
        actionedByCapturedName = actionedByDisplayName,
        source = source,
        activeCaseLoadId = activeCaseLoadId,
        affectedComponents = affectedComponents,
      )
      csipRecord.registerEntityEvent(
        CsipUpdatedEvent(
          recordUuid = csipRecord.recordUuid,
          prisonNumber = csipRecord.prisonNumber,
          description = description,
          occurredAt = actionedAt,
          source = source,
          updatedBy = actionedBy,
          affectedComponents = affectedComponents,
        ),
      )
    }

    return csipRecord
  }

  fun addContributoryFactor(
    createRequest: CreateContributoryFactorRequest,
    factorType: ReferenceData,
    actionedAt: LocalDateTime = LocalDateTime.now(),
    actionedBy: String,
    actionedByDisplayName: String,
    source: Source,
  ) = ContributoryFactor(
    referral = this,
    comment = createRequest.comment,
    contributoryFactorType = factorType,
    createdAt = actionedAt,
    createdBy = actionedBy,
    createdByDisplayName = actionedByDisplayName,
  ).apply {
    contributoryFactors.add(this)
    csipRecord.registerEntityEvent(
      ContributoryFactorCreatedEvent(
        entityUuid = contributoryFactorUuid,
        recordUuid = csipRecord.recordUuid,
        prisonNumber = csipRecord.prisonNumber,
        description = DomainEventType.CONTRIBUTORY_FACTOR_CREATED.description,
        occurredAt = actionedAt,
        source = source,
        updatedBy = actionedBy,
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
