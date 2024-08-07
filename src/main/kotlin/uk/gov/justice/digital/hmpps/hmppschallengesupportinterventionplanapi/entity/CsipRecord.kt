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
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Record
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreatePlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.PlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.verifyAllReferenceData
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class)
class CsipRecord(

  @Audited(withModifiedFlag = false)
  @Column(nullable = false, length = 10, updatable = false)
  val prisonNumber: String,

  @Audited(withModifiedFlag = false)
  @Column(length = 6, updatable = false)
  val prisonCodeWhenRecorded: String? = null,

  logCode: String? = null,

  @Audited(withModifiedFlag = false)
  @Column(unique = true, nullable = false)
  val recordUuid: UUID = UUID.randomUUID(),

  @Audited(withModifiedFlag = false)
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
  @NotAudited
  override var propertyChanges: MutableSet<PropertyChange> = mutableSetOf()

  @Column(length = 10)
  var logCode: String? = logCode
    set(value) {
      propertyChanged(::logCode, value)
      field = value
    }

  @Audited(withModifiedFlag = false)
  override var createdAt: LocalDateTime = LocalDateTime.now()

  @Audited(withModifiedFlag = false)
  @Column(length = 32)
  override var createdBy: String = SYSTEM_USER_NAME

  @Audited(withModifiedFlag = false)
  @Column(length = 255)
  override var createdByDisplayName: String = SYSTEM_DISPLAY_NAME

  @Audited(withModifiedFlag = false)
  override var lastModifiedAt: LocalDateTime? = null

  @Audited(withModifiedFlag = false)
  @Column(length = 32)
  override var lastModifiedBy: String? = null

  @Audited(withModifiedFlag = false)
  @Column(length = 255)
  override var lastModifiedByDisplayName: String? = null

  @NotAudited
  @Fetch(FetchMode.SELECT)
  @OneToOne(mappedBy = "csipRecord", cascade = [CascadeType.ALL])
  var referral: Referral? = null
    private set

  @NotAudited
  @Fetch(FetchMode.SELECT)
  @OneToOne(mappedBy = "csipRecord", cascade = [CascadeType.ALL])
  var plan: Plan? = null
    private set

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
    request.referral?.also { referral.update(it, referenceProvider) }
    val allChanges = propertyChanges + referral.propertyChanges
    if (allChanges.isNotEmpty()) {
      val affectedComponents = buildSet {
        if (propertyChanges.isNotEmpty()) add(Record)
        if (referral.propertyChanges.isNotEmpty()) add(AffectedComponent.Referral)
      }
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

  fun upsertPlan(context: CsipRequestContext, request: PlanRequest) = apply {
    val isNew = plan == null
    if (isNew) {
      plan = Plan(this, request.caseManager, request.reasonForPlan, request.firstCaseReviewDate)
    } else {
      plan!!.upsert(request)
    }

    if (isNew || plan!!.propertyChanges.isNotEmpty()) {
      val affectedComponents = buildSet {
        add(AffectedComponent.Plan)
        if (request is CreatePlanRequest && request.identifiedNeeds.isNotEmpty()) add(IdentifiedNeed)
      }
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
    affected.filter { it.value.isNotEmpty() }.map { entry ->
      when (entry.key) {
        Record -> listOf(
          CsipDeletedEvent(
            recordUuid = recordUuid,
            prisonNumber = prisonNumber,
            occurredAt = context.requestAt,
            source = context.source,
            affectedComponents = affected.keys,
          ),
        )

        ContributoryFactor, Interview, IdentifiedNeed, Review, Attendee -> entry.value.mapNotNull {
          createDeletedEvent(entry.key, prisonNumber, recordUuid, it, context.requestAt, context.source)
        }

        AffectedComponent.Referral, SaferCustodyScreeningOutcome, DecisionAndActions, Investigation, AffectedComponent.Plan -> listOf()
      }
    }.flatten().forEach(::registerEvent)
  }

  internal fun registerEntityEvent(event: DomainEventable): DomainEventable = registerEvent(event)

  private fun components(): Map<AffectedComponent, Set<UUID>> = buildMap {
    put(Record, setOf(recordUuid))
    referral?.also { putAll(it.components()) }
    plan?.also { putAll(it.components()) }
  }
}
