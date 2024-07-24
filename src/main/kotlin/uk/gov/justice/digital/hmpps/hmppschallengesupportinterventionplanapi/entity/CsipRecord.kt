package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toInitialReferralEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipCreatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.DomainEventable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
@EntityListeners(AuditedEntityListener::class)
class CsipRecord(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "record_id")
  val recordId: Long = 0,

  @Column(unique = true, nullable = false)
  val recordUuid: UUID = UUID.randomUUID(),

  @Column(nullable = false, length = 10)
  val prisonNumber: String,

  @Column(length = 6)
  val prisonCodeWhenRecorded: String? = null,

  logCode: String? = null,
  createdAt: LocalDateTime,
  createdBy: String,
  createdByDisplayName: String,

) : AbstractAggregateRoot<CsipRecord>(), PropertyChangeMonitor, Audited {

  @PostLoad
  fun resetPropertyChanges() {
    propertyChanges = mutableSetOf()
  }

  @Transient
  override var propertyChanges: MutableSet<PropertyChange> = mutableSetOf()

  @Column(length = 10)
  var logCode: String? = logCode
    set(value) {
      propertyChanged(::logCode, value)
      field = value
    }

  override var createdAt: LocalDateTime = createdAt
    private set

  @Column(length = 32)
  override var createdBy: String = createdBy
    private set

  @Column(length = 255)
  override var createdByDisplayName: String = createdByDisplayName
    private set

  override var lastModifiedAt: LocalDateTime? = null
    private set

  @Column(length = 32)
  override var lastModifiedBy: String? = null
    private set

  @Column(length = 255)
  override var lastModifiedByDisplayName: String? = null
    private set

  @OneToMany(
    mappedBy = "csipRecord",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  @OrderBy("actioned_at DESC")
  private val auditEvents: MutableList<AuditEvent> = mutableListOf()

  @OneToOne(
    mappedBy = "csipRecord",
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  var referral: Referral? = null

  fun referral() = referral

  fun auditEvents() = auditEvents.toList().sortedByDescending { it.actionedAt }

  internal fun addAuditEvent(
    context: CsipRequestContext,
    action: AuditEventAction,
    description: String,
    source: Source,
    activeCaseLoadId: String?,
    affectedComponents: Set<AffectedComponent>,
  ) = apply {
    auditEvents.add(
      AuditEvent(
        csipRecord = this,
        action = action,
        description = description,
        actionedAt = context.requestAt,
        actionedBy = context.username,
        actionedByCapturedName = context.userDisplayName,
        source = source,
        activeCaseLoadId = activeCaseLoadId,
        affectedComponents = affectedComponents,
      ),
    )
  }

  fun setReferral(referral: Referral) = apply {
    this.referral = referral
  }

  fun create(
    createCsipRecordRequest: CreateCsipRecordRequest,
    csipRequestContext: CsipRequestContext,
    incidentType: ReferenceData,
    incidentLocation: ReferenceData,
    referrerAreaOfWork: ReferenceData,
    incidentInvolvement: ReferenceData?,
    contributoryFactors: Map<String, ReferenceData>,
    releaseDate: LocalDate? = null,
    description: String = DomainEventType.CSIP_CREATED.description,
  ): CsipRecord = apply {
    referral = createCsipRecordRequest.toInitialReferralEntity(
      this,
      csipRequestContext,
      incidentType,
      incidentLocation,
      referrerAreaOfWork,
      incidentInvolvement,
      releaseDate,
    ).apply {
      createCsipRecordRequest.referral.contributoryFactors.forEach { factor ->
        addContributoryFactor(
          createRequest = factor,
          factorType = contributoryFactors[factor.factorTypeCode]!!,
          actionedAt = createdAt,
          actionedBy = createdBy,
          actionedByDisplayName = createdByDisplayName,
          source = csipRequestContext.source,
        )
      }
    }
    addAuditEvent(
      context = csipRequestContext,
      action = AuditEventAction.CREATED,
      description = "CSIP record created via referral with ${referral!!.contributoryFactors().size} contributory factors",
      source = csipRequestContext.source,
      activeCaseLoadId = csipRequestContext.activeCaseLoadId,
      affectedComponents = buildSet {
        addAll(setOf(AffectedComponent.Record, AffectedComponent.Referral))
        if (referral!!.contributoryFactors().isNotEmpty()) add(AffectedComponent.ContributoryFactor)
      },
    )
    registerEvent(
      CsipCreatedEvent(
        recordUuid = this.recordUuid,
        prisonNumber = this.prisonNumber,
        description = description,
        occurredAt = createdAt,
        source = csipRequestContext.source,
        createdBy = createdBy,
        affectedComponents = buildSet {
          addAll(setOf(AffectedComponent.Record, AffectedComponent.Referral))
          if (referral?.contributoryFactors()?.isNotEmpty() == true) add(AffectedComponent.ContributoryFactor)
        },
      ),
    )
  }

  fun update(
    context: CsipRequestContext,
    request: UpdateCsipRecordRequest,
    referenceProvider: (ReferenceDataType, String) -> ReferenceData,
  ): CsipRecord {
    val referral = requireNotNull(referral)
    logCode = request.logCode
    request.referral?.also { referral.update(context, it, referenceProvider) }
    val allChanges = propertyChanges + referral.propertyChanges
    if (allChanges.isNotEmpty()) {
      val affectedComponents = buildSet {
        if (propertyChanges.isNotEmpty()) add(AffectedComponent.Record)
        if (referral.propertyChanges.isNotEmpty()) add(AffectedComponent.Referral)
      }
      addAuditEvent(
        context = csipRequestContext(),
        action = AuditEventAction.UPDATED,
        description = auditDescription(propertyChanges, referral.propertyChanges),
        source = context.source,
        activeCaseLoadId = context.activeCaseLoadId,
        affectedComponents = affectedComponents,
      )
      registerEvent(
        CsipUpdatedEvent(
          recordUuid = recordUuid,
          prisonNumber = prisonNumber,
          description = CSIP_UPDATED.description,
          occurredAt = context.requestAt,
          source = context.source,
          updatedBy = context.username,
          affectedComponents = affectedComponents,
        ),
      )
    }
    return this
  }

  fun registerEntityEvent(event: DomainEventable): DomainEventable = registerEvent(event)

  override fun recordCreatedDetails(context: CsipRequestContext) {
    createdAt = context.requestAt
    createdBy = context.username
    createdByDisplayName = context.userDisplayName
  }

  override fun recordModifiedDetails(context: CsipRequestContext) {
    lastModifiedAt = context.requestAt
    lastModifiedBy = context.username
    lastModifiedByDisplayName = context.userDisplayName
  }

  private fun auditDescription(recordChanges: Set<PropertyChange>, referralChanges: Set<PropertyChange>): String {
    val recordDescription =
      if (recordChanges.isEmpty()) null else recordChanges.joinToString(prefix = "updated CSIP record ") { it.description() }
    val referralDescription =
      if (referralChanges.isEmpty()) null else referralChanges.joinToString(prefix = "updated referral ") { it.description() }
    return setOfNotNull(recordDescription, referralDescription).filter { it.isNotBlank() }
      .joinToString(separator = " and ").replaceFirstChar(Char::uppercaseChar)
  }
}
