package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table
data class SaferCustodyScreeningOutcome(
  @Id
  @MapsId("record_id")
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name = "record_id",
    referencedColumnName = "record_id",
  ) val csipRecord: CsipRecord,

  @ManyToOne
  @JoinColumn(
    name = "outcome_id",
    referencedColumnName = "reference_data_id",
  ) val outcomeType: ReferenceData,

  @Column(nullable = false, length = 100)
  val recordedBy: String,

  @Column(nullable = false, length = 255)
  val recordedByDisplayName: String,

  @Column(nullable = false)
  val date: LocalDate,

  @Column(nullable = false)
  val reasonForDecision: String,
) : AbstractAggregateRoot<SaferCustodyScreeningOutcome>() {
  fun create(
    description: String = "Safer custody screening outcome added to referral",
    createdAt: LocalDateTime = LocalDateTime.now(),
    createdBy: String,
    createdByDisplayName: String,
    source: Source,
    reason: Reason = Reason.USER,
    activeCaseLoadId: String?,
  ): CsipRecord = let {
    csipRecord.setSaferCustodyScreeningOutcome(this)
    csipRecord.addAuditEvent(
      action = AuditEventAction.UPDATED,
      description = description,
      actionedAt = createdAt,
      actionedBy = createdBy,
      actionedByCapturedName = createdByDisplayName,
      source = source,
      reason = reason,
      activeCaseLoadId = activeCaseLoadId,
      isSaferCustodyScreeningOutcomeAffected = true,
    )
    csipRecord.registerCsipEvent(
      CsipUpdatedEvent(
        recordUuid = csipRecord.recordUuid,
        prisonNumber = csipRecord.prisonNumber,
        description = description,
        occurredAt = createdAt,
        source = source,
        reason = reason,
        updatedBy = createdBy,
        isSaferCustodyScreeningOutcomeAffected = true,
      ),
    )
    return csipRecord
  }
}
