package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral

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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.SimpleVersion
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipChangedListener
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ScreeningOutcomeRequest
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@Audited(withModifiedFlag = true)
@EntityListeners(CsipChangedListener::class)
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
) : SimpleVersion(),
  CsipAware {
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

  internal fun update(request: ScreeningOutcomeRequest, rdSupplier: (ReferenceDataType, String) -> ReferenceData) = apply {
    outcome = rdSupplier(ReferenceDataType.SCREENING_OUTCOME_TYPE, request.outcomeTypeCode)
    date = request.date
    recordedBy = request.recordedBy
    recordedByDisplayName = request.recordedByDisplayName
    reasonForDecision = request.reasonForDecision
  }
}

fun SaferCustodyScreeningOutcome.toModel() = uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.SaferCustodyScreeningOutcome(
  outcome = outcome.toReferenceDataModel(),
  recordedBy = recordedBy,
  recordedByDisplayName = recordedByDisplayName,
  date = date,
  reasonForDecision = reasonForDecision,
)
