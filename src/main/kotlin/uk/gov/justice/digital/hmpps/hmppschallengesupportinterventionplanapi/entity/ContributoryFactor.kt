package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CommentRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, DeleteEventListener::class)
@BatchSize(size = 20)
class ContributoryFactor(
  @Audited(withModifiedFlag = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "referral_id")
  val referral: Referral,

  contributoryFactorType: ReferenceData,

  comment: String?,

  legacyId: Long? = null,
) : SimpleAuditable(), Identifiable, CsipAware {
  override fun csipRecord() = referral.csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "contributory_factor_id")
  override val id: UUID = newUuid()

  @Audited(withModifiedFlag = false)
  override var legacyId: Long? = legacyId
    private set

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "contributory_factor_type_id", updatable = false)
  var contributoryFactorType: ReferenceData = contributoryFactorType
    private set

  var comment: String? = comment
    private set

  fun update(request: ContributoryFactorRequest, rdSupplier: (ReferenceDataType, String) -> ReferenceData) = apply {
    update(request)
    contributoryFactorType = rdSupplier(ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE, request.factorTypeCode)
    if (request is LegacyIdAware) {
      legacyId = request.legacyId
    }
  }

  fun update(request: CommentRequest) = apply {
    comment = request.comment
  }
}
