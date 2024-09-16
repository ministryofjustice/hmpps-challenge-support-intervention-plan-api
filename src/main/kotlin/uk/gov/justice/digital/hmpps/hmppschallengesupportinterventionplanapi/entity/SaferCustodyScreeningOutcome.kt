package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ScreeningOutcomeRequest
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(AuditedEntityListener::class)
class SaferCustodyScreeningOutcome(
  @Audited(withModifiedFlag = false)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "safer_custody_screening_outcome_id")
  val referral: Referral,
  outcome: ReferenceData,
  date: LocalDate,
  recordedBy: String,
  recordedByDisplayName: String,
  reasonForDecision: String?,
) : SimpleAuditable(), CsipAware {
  override fun csipRecord() = referral.csipRecord

  @Audited(targetAuditMode = NOT_AUDITED, withModifiedFlag = true)
  @ManyToOne
  @JoinColumn(name = "outcome_id")
  var outcome: ReferenceData = outcome
    private set

  @Column(nullable = false)
  var date: LocalDate = date
    private set

  @Column(nullable = false, length = 100)
  var recordedBy: String = recordedBy
    private set

  @Column(nullable = false, length = 255)
  var recordedByDisplayName: String = recordedByDisplayName
    private set

  var reasonForDecision: String? = reasonForDecision
    private set

  @Audited(withModifiedFlag = false)
  @Id
  @Column(name = "safer_custody_screening_outcome_id")
  val id: UUID = referral.id

  fun update(request: ScreeningOutcomeRequest, rdSupplier: (ReferenceDataType, String) -> ReferenceData) = apply {
    outcome = rdSupplier(ReferenceDataType.SCREENING_OUTCOME_TYPE, request.outcomeTypeCode)
    date = request.date
    recordedBy = request.recordedBy
    recordedByDisplayName = request.recordedByDisplayName
    reasonForDecision = request.reasonForDecision
  }
}
