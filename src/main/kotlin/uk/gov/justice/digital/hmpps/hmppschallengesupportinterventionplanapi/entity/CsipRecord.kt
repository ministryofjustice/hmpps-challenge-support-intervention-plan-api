package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.SoftDelete
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.SYSTEM_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.SYSTEM_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toInitialReferralEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipCreatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDeletedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.DomainEventable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.EventFactory.createDeletedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Record
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.verifyAllReferenceData
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
@SoftDelete
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

  @Transient
  var auditEvents: MutableSet<AuditRequest>? = mutableSetOf()

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

  @Fetch(FetchMode.SELECT)
  @OneToOne(mappedBy = "csipRecord", cascade = [CascadeType.ALL])
  var referral: Referral? = null
    private set

  internal fun addAuditEvent(
    action: AuditEventAction,
    description: String,
    affectedComponents: Set<AffectedComponent>,
  ) = apply {
    auditEvents = (auditEvents ?: mutableSetOf())
    auditEvents!!.add(AuditRequest(action, description, affectedComponents))
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
      action = AuditEventAction.CREATED,
      description = "CSIP record created via referral with ${referral!!.contributoryFactors().size} contributory factors",
      affectedComponents = buildSet {
        addAll(setOf(Record, AffectedComponent.Referral))
        if (referral!!.contributoryFactors().isNotEmpty()) add(ContributoryFactor)
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
          addAll(setOf(Record, AffectedComponent.Referral))
          if (referral?.contributoryFactors()?.isNotEmpty() == true) add(ContributoryFactor)
        },
      ),
    )
  }

  fun update(
    context: CsipRequestContext,
    request: UpdateCsipRecordRequest,
    referenceProvider: (ReferenceDataType, String) -> ReferenceData,
  ): CsipRecord = apply {
    val referral = requireNotNull(referral)
    logCode = request.logCode
    request.referral?.also { referral.update(context, it, referenceProvider) }
    val allChanges = propertyChanges + referral.propertyChanges
    if (allChanges.isNotEmpty()) {
      val affectedComponents = buildSet {
        if (propertyChanges.isNotEmpty()) add(Record)
        if (referral.propertyChanges.isNotEmpty()) add(AffectedComponent.Referral)
      }
      addAuditEvent(
        action = AuditEventAction.UPDATED,
        description = auditDescription(propertyChanges, referral.propertyChanges),
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
  }

  fun delete(context: CsipRequestContext) = apply {
    val affected = components()
    addAuditEvent(
      action = AuditEventAction.DELETED,
      description = "CSIP deleted",
      affectedComponents = affected.keys,
    )
    affected.filter { it.value.isNotEmpty() }.map { entry ->
      when (entry.key) {
        Record -> listOf(
          CsipDeletedEvent(
            recordUuid = recordUuid,
            prisonNumber = prisonNumber,
            description = DomainEventType.CSIP_DELETED.description,
            occurredAt = context.requestAt,
            source = context.source,
            affectedComponents = affected.keys,
          ),
        )

        ContributoryFactor, Interview, IdentifiedNeed, Review, Attendee -> entry.value.mapNotNull {
          createDeletedEvent(entry.key, prisonNumber, recordUuid, it, context.requestAt, context.source)
        }

        AffectedComponent.Referral, SaferCustodyScreeningOutcome, DecisionAndActions, Investigation, Plan -> listOf()
      }
    }.flatten().forEach(::registerEvent)
  }

  internal fun registerEntityEvent(event: DomainEventable): DomainEventable = registerEvent(event)

  private fun auditDescription(recordChanges: Set<PropertyChange>, referralChanges: Set<PropertyChange>): String {
    val recordDescription =
      if (recordChanges.isEmpty()) null else recordChanges.joinToString(prefix = "updated CSIP record ") { it.description() }
    val referralDescription =
      if (referralChanges.isEmpty()) null else referralChanges.joinToString(prefix = "updated referral ") { it.description() }
    return setOfNotNull(recordDescription, referralDescription).filter { it.isNotBlank() }
      .joinToString(separator = " and ").replaceFirstChar(Char::uppercaseChar)
  }

  private fun components(): Map<AffectedComponent, Set<UUID>> = buildMap {
    put(Record, setOf(recordUuid))
    referral?.also { putAll(it.components()) }
  }
}
