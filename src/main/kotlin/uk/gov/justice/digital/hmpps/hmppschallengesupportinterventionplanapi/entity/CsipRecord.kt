package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toInitialReferralEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.PlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.verifyAllReferenceData
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, DeleteEventListener::class)
class CsipRecord(

  @Audited(withModifiedFlag = false)
  @Column(nullable = false, length = 10, updatable = false)
  val prisonNumber: String,

  @Audited(withModifiedFlag = false)
  @Column(length = 6, updatable = false)
  val prisonCodeWhenRecorded: String? = null,

  logCode: String? = null,
) : SimpleAuditable(), Identifiable {

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "record_id")
  override val id: UUID = newUuid()

  @Column(length = 10)
  var logCode: String? = logCode
    private set

  @NotAudited
  @OneToOne(mappedBy = "csipRecord", cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE])
  var referral: Referral? = null
    private set

  @NotAudited
  @OneToOne(mappedBy = "csipRecord", cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE])
  var plan: Plan? = null
    private set

  @NotAudited
  @Enumerated(EnumType.STRING)
  @Column(insertable = false, updatable = false)
  var status: CsipStatus = CsipStatus.UNKNOWN
    private set

  fun create(
    request: CreateCsipRecordRequest,
    csipRequestContext: CsipRequestContext,
    referenceDataRepository: ReferenceDataRepository,
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
        )
      }
    }
  }

  fun update(
    request: UpdateCsipRecordRequest,
    referenceProvider: (ReferenceDataType, String) -> ReferenceData,
  ): CsipRecord = apply {
    val referral = requireNotNull(referral)
    logCode = request.logCode
    request.referral?.also { referral.update(it, referenceProvider) }
  }

  fun upsertPlan(request: PlanRequest) = let {
    if (plan == null) {
      plan = Plan(this, request.caseManager, request.reasonForPlan, request.firstCaseReviewDate)
    } else {
      plan!!.upsert(request)
    }
    plan!!
  }

  fun components(): Set<CsipComponent> = buildSet {
    add(CsipComponent.RECORD)
    referral?.also { addAll(it.components()) }
    plan?.also { addAll(it.components()) }
  }
}
