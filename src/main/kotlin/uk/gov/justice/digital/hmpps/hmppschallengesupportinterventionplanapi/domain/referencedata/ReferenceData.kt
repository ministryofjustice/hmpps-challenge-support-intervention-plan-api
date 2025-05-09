package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
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
  @Embedded
  val key: ReferenceDataKey,
  val description: String,
  val listSequence: Int,
  val deactivatedAt: LocalDateTime?,
  @Id
  @Column(name = "reference_data_id")
  val id: Long = 0,
) : ReferenceDataLookup by key {
  fun isActive() = deactivatedAt?.isBefore(LocalDateTime.now()) != true

  companion object {
    const val SIGNED_OFF_BY_OTHER = "OTHER"
  }
}

interface ReferenceDataLookup {
  val domain: ReferenceDataType
  val code: String
}

@Embeddable
data class ReferenceDataKey(
  @Enumerated(EnumType.STRING)
  override val domain: ReferenceDataType,
  override val code: String,
) : ReferenceDataLookup

fun ReferenceData.toReferenceDataModel() = uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData(
  code = code,
  description = description,
  listSequence = listSequence,
  deactivatedAt = deactivatedAt,
)
