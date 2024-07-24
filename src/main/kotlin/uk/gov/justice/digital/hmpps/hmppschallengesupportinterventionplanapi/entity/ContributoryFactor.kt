package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "contributory_factor")
class ContributoryFactor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val contributoryFactorId: Long = 0,

  @Column(unique = true, nullable = false)
  val contributoryFactorUuid: UUID = UUID.randomUUID(),

  val comment: String? = null,

  createdAt: LocalDateTime,
  createdBy: String,
  createdByDisplayName: String,

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
    name = "referral_id",
    referencedColumnName = "referral_id",
  )
  val referral: Referral,

  @ManyToOne
  @JoinColumn(name = "contributory_factor_type_id", insertable = false, updatable = false)
  val contributoryFactorType: ReferenceData,
) : CsipChild, Audited {
  @Transient
  override val csipRecord: CsipRecord = referral.csipRecord

  @Column(nullable = false)
  override var createdAt: LocalDateTime = createdAt
    private set

  @Column(nullable = false, length = 32)
  override var createdBy: String = createdBy
    private set

  @Column(nullable = false, length = 255)
  override var createdByDisplayName: String = createdByDisplayName
    private set

  override var lastModifiedAt: LocalDateTime? = null
    private set

  @Column(length = 32)
  override var lastModifiedBy: String? = null
    private set

  @Column(length = 255)
  override var lastModifiedByDisplayName: String? = null

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
}
