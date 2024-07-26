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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.SYSTEM_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.SYSTEM_USER_NAME
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.verifyAllReferenceData
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
@EntityListeners(AuditedEntityListener::class)
class CsipRecord(

  @Column(nullable = false, length = 10)
  val prisonNumber: String,

  @Column(length = 6)
  val prisonCodeWhenRecorded: String? = null,

  logCode: String? = null,

  @Column(unique = true, nullable = false)
  val recordUuid: UUID = UUID.randomUUID(),

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "record_id")
  val id: Long = 0,
) : AbstractAggregateRoot<CsipRecord>(), PropertyChangeMonitor, Auditable {

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

  override var createdAt: LocalDateTime = LocalDateTime.now()

  @Column(length = 32)
  override var createdBy: String = SYSTEM_USER_NAME

  @Column(length = 255)
  override var createdByDisplayName: String = SYSTEM_DISPLAY_NAME

  override var lastModifiedAt: LocalDateTime? = null

  @Column(length = 32)
  override var lastModifiedBy: String? = null

  @Column(length = 255)
  override var lastModifiedByDisplayName: String? = null

  @OneToMany(
    mappedBy = "csipRecord",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  @OrderBy("actioned_at DESC")
  private val auditEvents: MutableList<AuditEvent> = mutableListOf()

  @OneToOne(mappedBy = "csipRecord", cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE])
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
    request: CreateCsipRecordRequest,
    csipRequestContext: CsipRequestContext,
    referenceDataRepository: ReferenceDataRepository,
    description: String = DomainEventType.CSIP_CREATED.description,
  ): CsipRecord = apply {
    referral = request.toInitialReferralEntity(
      this,
      csipRequestContext,
      referenceDataRepository,
    ).apply {
      val factorTypeCodes = request.referral.contributoryFactors.map { it.factorTypeCode }.toSet()
      val contributoryFactors =
        referenceDataRepository.verifyAllReferenceData(CONTRIBUTORY_FACTOR_TYPE, factorTypeCodes)
      request.referral.contributoryFactors.forEach { factor ->
        addContributoryFactor(
          createRequest = factor,
          factorType = requireNotNull(contributoryFactors[factor.factorTypeCode]),
          context = csipRequestContext,
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
          affectedComponents = affectedComponents,
        ),
      )
    }
    return this
  }

  fun registerEntityEvent(event: DomainEventable): DomainEventable = registerEvent(event)

  private fun auditDescription(recordChanges: Set<PropertyChange>, referralChanges: Set<PropertyChange>): String {
    val recordDescription =
      if (recordChanges.isEmpty()) null else recordChanges.joinToString(prefix = "updated CSIP record ") { it.description() }
    val referralDescription =
      if (referralChanges.isEmpty()) null else referralChanges.joinToString(prefix = "updated referral ") { it.description() }
    return setOfNotNull(recordDescription, referralDescription).filter { it.isNotBlank() }
      .joinToString(separator = " and ").replaceFirstChar(Char::uppercaseChar)
  }
}
