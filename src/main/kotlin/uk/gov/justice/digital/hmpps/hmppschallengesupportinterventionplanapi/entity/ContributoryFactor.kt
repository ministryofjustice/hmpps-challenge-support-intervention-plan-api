package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class)
class ContributoryFactor(
  @Audited(withModifiedFlag = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "referral_id")
  val referral: Referral,

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "contributory_factor_type_id", updatable = false)
  val contributoryFactorType: ReferenceData,

  val comment: String? = null,

  @Audited(withModifiedFlag = false)
  @Column(unique = true, nullable = false)
  val contributoryFactorUuid: UUID = UUID.randomUUID(),

  @Audited(withModifiedFlag = false)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "contributory_factor_id")
  val id: Long = 0,
) : SimpleAuditable(), Parented {
  override fun parent() = referral
}
