package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
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

  @Column(nullable = false)
  val createdAt: LocalDateTime,

  @Column(nullable = false, length = 32)
  val createdBy: String,

  @Column(nullable = false, length = 255)
  val createdByDisplayName: String,

  ) : AbstractAggregateRoot<CsipRecord>() {

  @PostLoad
  internal fun clearPropertyChanges() {
    propertyChanges = mutableSetOf()
  }

  @Transient
  private var propertyChanges: MutableSet<PropertyChange> = mutableSetOf()

  @Column(length = 10)
  var logCode: String? = logCode
    set(value) {
      listenForChanges("logCode", field, value)
      field = value
    }

  var lastModifiedAt: LocalDateTime? = null
    private set

  @Column(length = 32)
  var lastModifiedBy: String? = null
    private set

  @Column(length = 255)
  var lastModifiedByDisplayName: String? = null
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
    action: AuditEventAction,
    description: String,
    actionedAt: LocalDateTime = LocalDateTime.now(),
    actionedBy: String,
    actionedByCapturedName: String,
    source: Source,
    activeCaseLoadId: String?,
    affectedComponents: Set<AffectedComponent>,
  ) = apply {
    auditEvents.add(
      AuditEvent(
        csipRecord = this,
        action = action,
        description = description,
        actionedAt = actionedAt,
        actionedBy = actionedBy,
        actionedByCapturedName = actionedByCapturedName,
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
      action = AuditEventAction.CREATED,
      description = "CSIP record created via referral with ${referral!!.contributoryFactors().size} contributory factors",
      actionedAt = createdAt,
      actionedBy = createdBy,
      actionedByCapturedName = createdByDisplayName,
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
    val allChanges = propertyChanges + referral.propertyChanges()
    if (allChanges.isNotEmpty()) {
      recordModifiedDetails(context)
      val affectedComponents = buildSet {
        if (propertyChanges.isNotEmpty()) add(AffectedComponent.Record)
        if (referral.propertyChanges().isNotEmpty()) add(AffectedComponent.Referral)
      }
      addAuditEvent(
        action = AuditEventAction.UPDATED,
        description = auditDescription(propertyChanges, referral.propertyChanges()),
        actionedAt = context.requestAt,
        actionedBy = context.username,
        actionedByCapturedName = context.userDisplayName,
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

  private fun recordModifiedDetails(context: CsipRequestContext) {
    lastModifiedAt = context.requestAt
    lastModifiedBy = context.username
    lastModifiedByDisplayName = context.userDisplayName
  }

  private fun listenForChanges(name: String, old: Any?, new: Any?) {
    if (old != new) {
      propertyChanges.add(PropertyChange(name, old, new))
    }
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
