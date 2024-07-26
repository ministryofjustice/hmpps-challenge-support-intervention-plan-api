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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class SaferCustodyScreeningOutcome(
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "referral_id", referencedColumnName = "referral_id")
  val referral: Referral,

  @ManyToOne
  @JoinColumn(name = "outcome_id", referencedColumnName = "reference_data_id")
  val outcomeType: ReferenceData,

  @Column(nullable = false, length = 100)
  val recordedBy: String,

  @Column(nullable = false, length = 255)
  val recordedByDisplayName: String,

  @Column(nullable = false)
  val date: LocalDate,

  @Column(nullable = false)
  val reasonForDecision: String,

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "safer_custody_screening_outcome_id")
  val id: Long = 0,

) : SimpleAuditable(), Parented {
  override fun parent() = referral
}
