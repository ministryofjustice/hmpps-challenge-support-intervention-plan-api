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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
data class Interview(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "interview_id") val interviewId: Long = 0,

  @Column(unique = true, nullable = false) val interviewUuid: UUID = UUID.randomUUID(),

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
    name = "record_id",
    referencedColumnName = "record_id",
  ) val investigation: Investigation,

  @Column(length = 100)
  var interviewee: String,

  var interviewDate: LocalDate,

  @ManyToOne @JoinColumn(
    name = "interviewee_role_id",
    referencedColumnName = "reference_data_id",
  )
  var intervieweeRole: ReferenceData,

  var interviewText: String?,

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
)
