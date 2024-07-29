package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import java.time.LocalDateTime

@Immutable
@Entity
@Table
class ReferenceData(
  @Enumerated(EnumType.STRING)
  val domain: ReferenceDataType,

  val code: String,
  val description: String,
  val listSequence: Int,
  val deactivatedAt: LocalDateTime?,
  @Id
  @Column(name = "reference_data_id")
  val id: Long = 0,
) {
  fun isActive() = deactivatedAt?.isBefore(LocalDateTime.now()) != true
}
