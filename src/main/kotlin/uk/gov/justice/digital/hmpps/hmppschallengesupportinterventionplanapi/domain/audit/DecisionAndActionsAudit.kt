package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.DecisionAndActions as DecisionAndActionsModel

@Immutable
@Entity
class DecisionAndActionsAudit(
  val date: LocalDate?,
  @ManyToOne
  @JoinColumn(name = "outcome_id")
  val outcome: ReferenceData,
  @ManyToOne
  @JoinColumn(name = "signed_off_by_role_id")
  val signedOffBy: ReferenceData?,
  val conclusion: String?,
  @Column(name = "recorded_by")
  val recordedBy: String?,
  @Column(name = "recorded_by_display_name")
  val recordedByDisplayName: String?,
  @Column(name = "next_steps")
  val nextSteps: String?,
  @Column(name = "action_other")
  val actionOther: String?,
  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  val actions: Set<DecisionAction>,
  @EmbeddedId
  val id: DecisionAndActionsAuditKey,
)

@Embeddable
data class DecisionAndActionsAuditKey(
  @Column(name = "rev_id")
  val revisionNumber: Long,
  @Column(name = "decision_and_actions_id")
  val uuid: UUID,
)

fun DecisionAndActionsAudit.toModel() = DecisionAndActionsModel(
  conclusion,
  outcome.toReferenceDataModel(),
  signedOffBy?.toReferenceDataModel(),
  recordedBy,
  recordedByDisplayName,
  date,
  nextSteps,
  actions,
  actionOther,
)

interface DecisionAndActionsAuditRepository : JpaRepository<DecisionAndActionsAudit, UUID> {
  fun findAllByIdUuid(uuid: UUID): List<DecisionAndActionsAudit>
}
