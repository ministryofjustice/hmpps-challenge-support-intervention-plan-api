package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime

@Entity
@Table
class AuditEvent(
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  val id: Long = 0,

  val csipRecordId: Long,

  @Enumerated(EnumType.STRING)
  val action: AuditEventAction,

  val description: String,
  val actionedAt: LocalDateTime,
  val actionedBy: String,
  val actionedByCapturedName: String,

  @Enumerated(EnumType.STRING)
  val source: Source,

  val activeCaseLoadId: String?,

  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  val affectedComponents: Set<AffectedComponent>,
)

interface AuditEventRepository : JpaRepository<AuditEvent, Long>
