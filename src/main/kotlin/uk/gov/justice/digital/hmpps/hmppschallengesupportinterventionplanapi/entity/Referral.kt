package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.AffectedComponents
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.ContributoryFactorCreatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DecisionAndActionsAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvestigationAlreadyExistsException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.SaferCustodyScreeningOutcomeAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table
data class Referral(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "referral_id")
  val referralId: Long = 0,

  @OneToOne(fetch = FetchType.LAZY) @JoinColumn(
    name = "record_id",
    referencedColumnName = "record_id",
  ) val csipRecord: CsipRecord,

  @Column(nullable = false) val incidentDate: LocalDate,

  @Column val incidentTime: LocalTime? = null,

  @Column(nullable = false, length = 240) val referredBy: String,

  @Column(nullable = false) val referralDate: LocalDate,

  @Column val referralSummary: String? = null,

  @Column val proactiveReferral: Boolean? = null,

  @Column val staffAssaulted: Boolean? = null,

  @Column val assaultedStaffName: String? = null,

  @Column val releaseDate: LocalDate? = null,

  @Column val descriptionOfConcern: String?,

  @Column val knownReasons: String?,

  @Column val otherInformation: String? = null,

  @Column val saferCustodyTeamInformed: Boolean? = null,

  @Column val referralComplete: Boolean? = null,

  @Column(length = 32) val referralCompletedBy: String? = null,

  @Column(length = 255) val referralCompletedByDisplayName: String? = null,

  @Column val referralCompletedDate: LocalDate? = null,

  @ManyToOne @JoinColumn(
    name = "incident_type_id",
    referencedColumnName = "reference_data_id",
  ) val incidentType: ReferenceData,

  @ManyToOne @JoinColumn(
    name = "incident_location_id",
    referencedColumnName = "reference_data_id",
  ) val incidentLocation: ReferenceData,

  @ManyToOne @JoinColumn(
    name = "referer_area_of_work_id",
    referencedColumnName = "reference_data_id",
  ) val refererAreaOfWork: ReferenceData,

  @ManyToOne @JoinColumn(
    name = "incident_involvement_id",
    referencedColumnName = "reference_Data_id",
  ) val incidentInvolvement: ReferenceData?,
) : AbstractAggregateRoot<Referral>() {
  @OneToOne(
    mappedBy = "referral",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  private var decisionAndActions: DecisionAndActions? = null

  fun decisionAndActions() = decisionAndActions

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
    reason: Reason = Reason.USER,
    actionOpenCsipAlert: Boolean,
    actionNonAssociationsUpdated: Boolean,
    actionObservationBook: Boolean,
    actionUnitOrCellMove: Boolean,
    actionCsraOrRsraReview: Boolean,
    actionServiceReferral: Boolean,
    actionSimReferral: Boolean,
    description: String = "Decision and actions added to referral",
  ): CsipRecord {
    if (decisionAndActions != null) {
      throw DecisionAndActionsAlreadyExistException(csipRecord.recordUuid)
    }

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

    with(csipRecord) {
      addAuditEvent(
        action = AuditEventAction.CREATED,
        description,
        actionedAt,
        actionedBy = decisionOutcomeRecordedBy,
        actionedByCapturedName = decisionOutcomeRecordedByDisplayName,
        source,
        reason,
        activeCaseLoadId,
        isDecisionAndActionsAffected = true,
      )
      registerEntityEvent(
        CsipUpdatedEvent(
          recordUuid = csipRecord.recordUuid,
          prisonNumber = csipRecord.prisonNumber,
          description = description,
          occurredAt = actionedAt,
          source = source,
          updatedBy = decisionOutcomeRecordedBy,
          AffectedComponents(isDecisionAndActionsAffected = true),
        ),
      )
    }

    return csipRecord
  }

  @OneToOne(
    mappedBy = "referral",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  private var saferCustodyScreeningOutcome: SaferCustodyScreeningOutcome? = null

  @OneToOne(
    mappedBy = "referral",
    fetch = FetchType.LAZY,
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
    if (saferCustodyScreeningOutcome != null) {
      throw SaferCustodyScreeningOutcomeAlreadyExistException(csipRecord.recordUuid)
    }
    val reason = Reason.USER
    val description = "Safer custody screening outcome added to referral"

    saferCustodyScreeningOutcome = SaferCustodyScreeningOutcome(
      referral = this,
      outcomeType = outcomeType,
      recordedBy = actionedBy,
      recordedByDisplayName = actionedByDisplayName,
      date = date,
      reasonForDecision = reasonForDecision,
    )

    with(csipRecord) {
      addAuditEvent(
        action = AuditEventAction.CREATED,
        description = description,
        actionedAt = actionedAt,
        actionedBy = actionedBy,
        actionedByCapturedName = actionedByDisplayName,
        source = source,
        reason = reason,
        activeCaseLoadId = activeCaseLoadId,
        isSaferCustodyScreeningOutcomeAffected = true,
      )
      registerEntityEvent(
        CsipUpdatedEvent(
          recordUuid = csipRecord.recordUuid,
          prisonNumber = csipRecord.prisonNumber,
          description = description,
          occurredAt = actionedAt,
          source = source,
          updatedBy = actionedBy,
          AffectedComponents(isSaferCustodyScreeningOutcomeAffected = true),
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
    if (investigation != null) {
      throw InvestigationAlreadyExistsException(csipRecord.recordUuid)
    }
    val reason = Reason.USER
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
        intervieweeRole = intervieweeRoleMap.get(interview.intervieweeRoleCode)!!,
        actionedAt = actionedAt,
        actionedBy = actionedBy,
        actionedByDisplayName = actionedByDisplayName,
        source = source,
      )
    }

    val isInterviewAffected = (investigation?.interviews()?.size ?: 0) > 0

    with(csipRecord) {
      addAuditEvent(
        action = AuditEventAction.CREATED,
        description = description,
        actionedAt = actionedAt,
        actionedBy = actionedBy,
        actionedByCapturedName = actionedByDisplayName,
        source = source,
        reason = reason,
        activeCaseLoadId = activeCaseLoadId,
        isInvestigationAffected = true,
        isInterviewAffected = isInterviewAffected,
      )
      registerEntityEvent(
        CsipUpdatedEvent(
          recordUuid = csipRecord.recordUuid,
          prisonNumber = csipRecord.prisonNumber,
          description = description,
          occurredAt = actionedAt,
          source = source,
          updatedBy = actionedBy,
          AffectedComponents(
            isInvestigationAffected = true,
            isInterviewAffected = isInterviewAffected,
          ),
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
}
