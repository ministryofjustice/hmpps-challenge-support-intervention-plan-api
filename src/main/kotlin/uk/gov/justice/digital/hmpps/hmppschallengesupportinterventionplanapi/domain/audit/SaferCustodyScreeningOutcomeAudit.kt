package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.SaferCustodyScreeningOutcome
import java.time.LocalDate
import java.util.UUID

@Immutable
@Entity
class SaferCustodyScreeningOutcomeAudit(
  @ManyToOne
  @JoinColumn(name = "outcome_id")
  val outcome: ReferenceData,
  val date: LocalDate,
  @Column(name = "recorded_by")
  val recordedBy: String,
  @Column(name = "recorded_by_display_name")
  val recordedByDisplayName: String,
  @Column(name = "reason_for_decision")
  val reasonForDecision: String?,
  @EmbeddedId
  val id: ScreeningOutcomeAuditKey,
)

@Embeddable
data class ScreeningOutcomeAuditKey(
  @Column(name = "rev_id")
  val revisionNumber: Long,
  @Column(name = "safer_custody_screening_outcome_id")
  val uuid: UUID,
)

fun SaferCustodyScreeningOutcomeAudit.toModel() = SaferCustodyScreeningOutcome(
  outcome = outcome.toReferenceDataModel(),
  recordedBy = recordedBy,
  recordedByDisplayName = recordedByDisplayName,
  date = date,
  reasonForDecision = reasonForDecision,
)

interface SaferCustodyScreeningOutcomeAuditRepository : JpaRepository<SaferCustodyScreeningOutcomeAudit, ScreeningOutcomeAuditKey> {
  fun findAllByIdUuid(uuid: UUID): List<SaferCustodyScreeningOutcomeAudit>
}
