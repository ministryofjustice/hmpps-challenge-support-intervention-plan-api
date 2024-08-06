package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SoftDelete
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import java.time.LocalDate

@Entity
@Table
@Audited
@SoftDelete
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class SaferCustodyScreeningOutcome(
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "safer_custody_screening_outcome_id")
  val referral: Referral,

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "outcome_id")
  val outcome: ReferenceData,

  @Audited(withModifiedFlag = true)
  @Column(nullable = false, length = 100)
  val recordedBy: String,

  @Audited(withModifiedFlag = true)
  @Column(nullable = false, length = 255)
  val recordedByDisplayName: String,

  @Audited(withModifiedFlag = true)
  @Column(nullable = false)
  val date: LocalDate,

  @Audited(withModifiedFlag = true)
  @Column(nullable = false)
  val reasonForDecision: String,

  @Id
  @Column(name = "safer_custody_screening_outcome_id")
  val id: Long = 0,

  ) : SimpleAuditable(), Parented {
  override fun parent() = referral
}
