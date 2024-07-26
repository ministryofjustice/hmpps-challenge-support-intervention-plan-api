package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import java.time.LocalDateTime

@Entity
@Table
class ReferenceData(
  @Enumerated(EnumType.STRING)
  val domain: ReferenceDataType,

  val code: String,
  var description: String,
  var listSequence: Int,
  val createdAt: LocalDateTime,
  val createdBy: String,
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reference_data_id")
  val id: Long = 0,
) {
  var lastModifiedAt: LocalDateTime? = null
  var lastModifiedBy: String? = null
  var deactivatedAt: LocalDateTime? = null
  var deactivatedBy: String? = null

  fun isActive() = deactivatedAt?.isBefore(LocalDateTime.now()) != true
}
