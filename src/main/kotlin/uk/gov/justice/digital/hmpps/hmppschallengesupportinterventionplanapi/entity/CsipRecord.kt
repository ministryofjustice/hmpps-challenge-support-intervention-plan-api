package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
data class CsipRecord(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val recordId: Long = 0,

  @Column(unique = true, nullable = false)
  val recordUuid: UUID,

  @Column(nullable = false, length = 10)
  val prisonNumber: String,

  @Column(length = 6)
  val prisonCodeWhenRecorded: String? = null,

  @Column(length = 10)
  val logNumber: String? = null,

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
