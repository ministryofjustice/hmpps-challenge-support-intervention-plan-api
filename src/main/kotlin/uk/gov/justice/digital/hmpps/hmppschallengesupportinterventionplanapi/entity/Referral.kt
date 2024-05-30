package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table
data class Referral(
  @ManyToOne
  @JoinColumn(name = "record_id")
  @Id
  val csipRecord: CsipRecord,

  @Column(nullable = false)
  val incidentDate: LocalDate,

  @Column
  val incidentTime: LocalTime? = null,

  @Column(nullable = false, length = 240)
  val referredBy: String,

  @Column(nullable = false)
  val referralDate: LocalDate,

  @Column
  val referralSummary: String? = null,

  @Column
  val proactiveReferral: Boolean? = null,

  @Column
  val staffAssaulted: Boolean? = null,

  @Column
  val assaultedStaffName: String? = null,

  @Column
  val releaseDate: LocalDate? = null,

  @Column(nullable = false)
  val descriptionOfConcern: String,

  @Column(nullable = false)
  val knownReasons: String,

  @Column
  val otherInformation: String? = null,

  @Column
  val saferCustodyTeamInformed: Boolean? = null,

  @Column
  val referralComplete: Boolean? = null,

  @Column(length = 32)
  val referralCompletedBy: String? = null,

  @Column(length = 255)
  val referralCompletedByDisplayName: String? = null,

  @Column
  val referralCompletedDate: LocalDate? = null,

  @ManyToOne
  @JoinColumn(name = "reference_data_id", insertable = false, updatable = false)
  val incidentType: ReferenceData,

  @ManyToOne
  @JoinColumn(name = "reference_data_id", insertable = false, updatable = false)
  val incidentLocation: ReferenceData,

  @ManyToOne
  @JoinColumn(name = "reference_data_id", insertable = false, updatable = false)
  val refererAreaOfWork: ReferenceData,

  @ManyToOne
  @JoinColumn(name = "reference_Data_id", insertable = false, updatable = false)
  val incidentInvolvement: ReferenceData,

)
