package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class, DeleteEventListener::class)
class Interview(
  @Audited(withModifiedFlag = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "investigation_id")
  val investigation: Investigation,

  @Column(length = 100)
  val interviewee: String,

  val interviewDate: LocalDate,

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "interviewee_role_id")
  val intervieweeRole: ReferenceData,

  val interviewText: String?,
) : SimpleAuditable(), Identifiable, CsipAware {
  override fun csipRecord() = investigation.referral.csipRecord

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "interview_id")
  override val id: UUID = newUuid()
}
