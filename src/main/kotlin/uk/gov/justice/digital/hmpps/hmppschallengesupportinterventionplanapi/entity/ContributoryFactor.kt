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
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "contributory_factor")
data class ContributoryFactor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val contributoryFactorId: Long = 0,

  @Column(unique = true, nullable = false)
  val contributoryFactorUuid: UUID = UUID.randomUUID(),

  val comment: String? = null,

  @Column(nullable = false)
  val createdAt: LocalDateTime,

  @Column(nullable = false, length = 32)
  val createdBy: String,

  @Column(nullable = false, length = 255)
  val createdByDisplayName: String,

  val lastModifiedAt: LocalDateTime? = null,

  @Column(length = 32)
  val lastModifiedBy: String? = null,

  @Column(length = 255)
  val lastModifiedByDisplayName: String? = null,

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
    name = "referral_id",
    referencedColumnName = "referral_id",
  )
  val referral: Referral,

  @ManyToOne
  @JoinColumn(name = "contributory_factor_type_id", insertable = false, updatable = false)
  val contributoryFactorType: ReferenceData,
)
