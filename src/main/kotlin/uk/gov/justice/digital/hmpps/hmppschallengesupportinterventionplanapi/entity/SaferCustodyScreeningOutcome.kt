package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table
data class SaferCustodyScreeningOutcome(
  @Id
  @MapsId("record_id")
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name = "record_id",
    referencedColumnName = "record_id",
  ) val referral: Referral,

  @ManyToOne
  @JoinColumn(
    name = "outcome_id",
    referencedColumnName = "reference_data_id",
  ) val outcomeType: ReferenceData,

  @Column(nullable = false, length = 100)
  val recordedBy: String,

  @Column(nullable = false, length = 255)
  val recordedByDisplayName: String,

  @Column(nullable = false)
  val date: LocalDate,

  @Column(nullable = false)
  val reasonForDecision: String,
)
