package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ValidDecisionDetail
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ValidInvestigationDetail
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ValidInvestigationDetail.Companion.WITH_INTERVIEW_MESSAGE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.DecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.InterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.InterviewsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.InvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDate
import java.util.UUID

@ValidInvestigationDetail(message = WITH_INTERVIEW_MESSAGE)
data class SyncInvestigationRequest(
  override val staffInvolved: String?,
  override val evidenceSecured: String?,
  override val occurrenceReason: String?,
  override val personsUsualBehaviour: String?,
  override val personsTrigger: String?,
  override val protectiveFactors: String?,
  @field:Valid
  override val interviews: List<SyncInterviewRequest>,
) : InvestigationRequest,
  InterviewsRequest {
  fun findRequiredReferenceDataKeys(): Set<ReferenceDataKey> = interviews.flatMap { it.findRequiredReferenceDataKeys() }.toSet()

  fun requestMappings(): Set<RequestMapping> = buildSet {
    addAll(interviews.map { RequestMapping(CsipComponent.INTERVIEW, it.legacyId, it.id) })
  }
}

data class SyncInterviewRequest(
  override val interviewee: String,
  override val interviewDate: LocalDate,
  override val intervieweeRoleCode: String,
  override val interviewText: String?,
  override val legacyId: Long,
  override val id: UUID?,
) : NomisIdentifiable,
  InterviewRequest,
  LegacyIdAware {
  fun findRequiredReferenceDataKeys(): Set<ReferenceDataKey> = setOf(ReferenceDataKey(ReferenceDataType.INTERVIEWEE_ROLE, intervieweeRoleCode))
}

@ValidDecisionDetail
data class SyncDecisionAndActionsRequest(
  override val conclusion: String?,
  override val outcomeTypeCode: String?,

  override val signedOffByRoleCode: String?,
  override val date: LocalDate?,
  override val recordedBy: String?,
  override val recordedByDisplayName: String?,
  override val nextSteps: String?,

  override val actionOther: String?,
  override val actions: Set<DecisionAction>,
) : DecisionAndActionsRequest {
  fun findRequiredReferenceDataKeys(): Set<ReferenceDataKey> = buildSet {
    outcomeTypeCode?.also {
      add(ReferenceDataKey(ReferenceDataType.DECISION_OUTCOME_TYPE, it))
      if (signedOffByRoleCode == null) {
        add(ReferenceDataKey(ReferenceDataType.DECISION_SIGNER_ROLE, ReferenceData.SIGNED_OFF_BY_OTHER))
      }
    }
    signedOffByRoleCode?.also { add(ReferenceDataKey(ReferenceDataType.DECISION_SIGNER_ROLE, it)) }
  }
}
