package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import java.time.LocalDateTime

@Entity
@Table
data class ReferenceData(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val referenceDataId: Long = 0,

  @Enumerated(EnumType.STRING)
  val domain: ReferenceDataType,
  val code: String,
  var description: String,
  var listSequence: Int,
  val createdAt: LocalDateTime,
  val createdBy: String,
) : AbstractAggregateRoot<ReferenceData>() {
  @Column(name = "last_modified_at")
  var modifiedAt: LocalDateTime? = null

  @Column(name = "last_modified_by")
  var modifiedBy: String? = null
  var deactivatedAt: LocalDateTime? = null
  var deactivatedBy: String? = null

  fun isActive() = deactivatedAt?.isBefore(LocalDateTime.now()) != true
}
