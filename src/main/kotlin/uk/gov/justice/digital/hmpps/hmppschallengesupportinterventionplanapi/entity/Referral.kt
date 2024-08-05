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
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.SoftDelete
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.InvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateReferral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertDecisionAndActionsRequest
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity
@Table
@SoftDelete
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class Referral(
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
  @Id
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

  @Fetch(FetchMode.SELECT)
  @OneToOne(mappedBy = "referral", cascade = [CascadeType.ALL])
  var saferCustodyScreeningOutcome: SaferCustodyScreeningOutcome? = null
    private set

  @Fetch(FetchMode.SELECT)
  @OneToOne(mappedBy = "referral", cascade = [CascadeType.ALL])
  var decisionAndActions: DecisionAndActions? = null
    private set

  @Fetch(FetchMode.SELECT)
  @OneToOne(mappedBy = "referral", cascade = [CascadeType.ALL])
  var investigation: Investigation? = null
    private set

  @OneToMany(mappedBy = "referral", cascade = [CascadeType.ALL])
  private var contributoryFactors: MutableList<ContributoryFactor> = mutableListOf()

  fun contributoryFactors() = contributoryFactors.toList().sortedByDescending { it.id }

  var incidentDate: LocalDate = incidentDate
    set(value) {
      propertyChanged(::incidentDate, value)
      field = value
    }

  var incidentTime: LocalTime? = incidentTime
    private set(value) {
      propertyChanged(::incidentTime, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "incident_type_id")
  var incidentType: ReferenceData = incidentType
    private set(value) {
      referenceDataChanged(::incidentType, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "incident_location_id")
  var incidentLocation: ReferenceData = incidentLocation
    private set(value) {
      referenceDataChanged(::incidentLocation, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "referer_area_of_work_id")
  var refererAreaOfWork: ReferenceData = refererAreaOfWork
    private set(value) {
      referenceDataChanged(::refererAreaOfWork, value)
      field = value
    }

  @ManyToOne
  @JoinColumn(name = "incident_involvement_id")
  var incidentInvolvement: ReferenceData? = incidentInvolvement
    private set(value) {
      referenceDataChanged(::incidentInvolvement, value)
      field = value
    }

  @Column(nullable = false, length = 240)
  var referredBy: String = referredBy
    private set(value) {
      propertyChanged(::referredBy, value)
      field = value
    }

  var proactiveReferral: Boolean? = proactiveReferral
    private set(value) {
      propertyChanged(::proactiveReferral, value)
      field = value
    }

  var staffAssaulted: Boolean? = staffAssaulted
    private set(value) {
      propertyChanged(::staffAssaulted, value)
      field = value
    }

  var assaultedStaffName: String? = assaultedStaffName
    private set(value) {
      propertyChanged(::assaultedStaffName, value)
      field = value
    }

  var descriptionOfConcern: String? = descriptionOfConcern
    private set(value) {
      propertyChanged(::descriptionOfConcern, value)
      field = value
    }

  var knownReasons: String? = knownReasons
    private set(value) {
      propertyChanged(::knownReasons, value)
      field = value
    }

  var otherInformation: String? = otherInformation
    private set(value) {
      propertyChanged(::otherInformation, value)
      field = value
    }

  @Enumerated(EnumType.STRING)
  var saferCustodyTeamInformed: OptionalYesNoAnswer = saferCustodyTeamInformed
    private set(value) {
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

  fun upsertDecisionAndActions(
    context: CsipRequestContext,
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

    if (isNew || decisionAndActions!!.propertyChanges.isNotEmpty()) {
      val affectedComponents = setOf(AffectedComponent.DecisionAndActions)
      val auditDescription = if (isNew) {
        "Decision and actions added to referral"
      } else {
        auditDescription(decisionAndActions!!.propertyChanges, prefix = "Updated decision and actions ")
      }
      csipRecord.addAuditEvent(
        if (isNew) AuditEventAction.CREATED else AuditEventAction.UPDATED,
        auditDescription,
        affectedComponents,
      )
      csipRecord.registerEntityEvent(
        CsipUpdatedEvent(
          recordUuid = csipRecord.recordUuid,
          prisonNumber = csipRecord.prisonNumber,
          occurredAt = context.requestAt,
          source = context.source,
          affectedComponents = affectedComponents,
        ),
      )
    }
    return csipRecord
  }

  fun createSaferCustodyScreeningOutcome(
    context: CsipRequestContext,
    request: CreateSaferCustodyScreeningOutcomeRequest,
    outcomeType: ReferenceData,
  ): CsipRecord {
    verifySaferCustodyScreeningOutcomeDoesNotExist()
    val description = "Safer custody screening outcome added to referral"

    saferCustodyScreeningOutcome = SaferCustodyScreeningOutcome(
      referral = this,
      outcomeType = outcomeType,
      recordedBy = request.recordedBy,
      recordedByDisplayName = request.recordedByDisplayName,
      date = request.date,
      reasonForDecision = request.reasonForDecision,
    )

    val affectedComponents = setOf(AffectedComponent.SaferCustodyScreeningOutcome)
    csipRecord.addAuditEvent(
      action = AuditEventAction.CREATED,
      description = description,
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

  fun upsertInvestigation(
    context: CsipRequestContext,
    request: InvestigationRequest,
  ): CsipRecord {
    val isNew = investigation == null
    if (isNew) {
      investigation = Investigation(this)
    }
    investigation!!.upsert(request)

    if (isNew || investigation!!.propertyChanges.isNotEmpty()) {
      val auditDescription = if (isNew) {
        "Investigation added to referral"
      } else {
        auditDescription(investigation!!.propertyChanges, prefix = "Updated investigation ")
      }
      val affectedComponents = setOf(AffectedComponent.Investigation)
      csipRecord.addAuditEvent(
        action = if (isNew) AuditEventAction.CREATED else AuditEventAction.UPDATED,
        description = auditDescription,
        affectedComponents = affectedComponents,
      )
      csipRecord.registerEntityEvent(
        CsipUpdatedEvent(
          recordUuid = csipRecord.recordUuid,
          prisonNumber = csipRecord.prisonNumber,
          occurredAt = context.requestAt,
          source = context.source,
          affectedComponents = affectedComponents,
        ),
      )
    }
    return csipRecord
  }

  fun addContributoryFactor(
    createRequest: CreateContributoryFactorRequest,
    factorType: ReferenceData,
    context: CsipRequestContext,
    auditRequest: AuditRequest? = null,
  ) = ContributoryFactor(
    referral = this,
    contributoryFactorType = factorType,
    comment = createRequest.comment,
  ).apply {
    contributoryFactors.add(this)
    auditRequest?.also { csipRecord.addAuditEvent(auditRequest) }
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

  internal fun components(): Map<AffectedComponent, Set<UUID>> = buildMap {
    put(AffectedComponent.Referral, setOf())
    saferCustodyScreeningOutcome?.also { put(AffectedComponent.SaferCustodyScreeningOutcome, setOf()) }
    decisionAndActions?.also { put(AffectedComponent.DecisionAndActions, setOf()) }
    investigation?.also { putAll(it.components()) }
    if (contributoryFactors.isNotEmpty()) {
      put(AffectedComponent.ContributoryFactor, contributoryFactors.map { it.contributoryFactorUuid }.toSet())
    }
  }

  private fun auditDescription(propertyChanges: Set<PropertyChange>, prefix: String): String =
    propertyChanges.joinToString(prefix = prefix) { it.description() }
}
