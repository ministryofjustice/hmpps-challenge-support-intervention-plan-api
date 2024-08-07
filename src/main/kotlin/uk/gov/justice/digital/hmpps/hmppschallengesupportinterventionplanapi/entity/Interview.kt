package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SoftDelete
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited
@SoftDelete
@EntityListeners(AuditedEntityListener::class, UpdateParentEntityListener::class)
class Interview(
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "investigation_id")
  val investigation: Investigation,

  @Audited(withModifiedFlag = true)
  @Column(length = 100)
  var interviewee: String,

  @Audited(withModifiedFlag = true)
  var interviewDate: LocalDate,

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "interviewee_role_id")
  var intervieweeRole: ReferenceData,

  @Audited(withModifiedFlag = true)
  var interviewText: String?,

  @Column(unique = true, nullable = false)
  val interviewUuid: UUID = UUID.randomUUID(),

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "interview_id")
  val id: Long = 0,
) : SimpleAuditable(), Parented {
  override fun parent() = investigation
}
