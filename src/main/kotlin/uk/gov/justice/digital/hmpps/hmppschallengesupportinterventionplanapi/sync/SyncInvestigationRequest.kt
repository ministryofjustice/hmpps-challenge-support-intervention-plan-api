package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ValidDecisionDetail
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ValidInvestigationDetail
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ValidInvestigationDetail.Companion.WITH_INTERVIEW_MESSAGE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.DecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.InterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.InterviewsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.InvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDate
import java.util.UUID

@ValidInvestigationDetail(message = WITH_INTERVIEW_MESSAGE)
data class SyncInvestigationRequest(
  @field:Size(min = 0, max = 4000, message = "Staff involved must be <= 4000 characters")
  override val staffInvolved: String?,
  @field:Size(min = 0, max = 4000, message = "Evidence secured must be <= 4000 characters")
  override val evidenceSecured: String?,
  @field:Size(min = 0, max = 4000, message = "Occurrence reason must be <= 4000 characters")
  override val occurrenceReason: String?,
  @field:Size(min = 0, max = 4000, message = "Person's usual behaviour must be <= 4000 characters")
  override val personsUsualBehaviour: String?,
  @field:Size(min = 0, max = 4000, message = "Person's trigger must be <= 4000 characters")
  override val personsTrigger: String?,
  @field:Size(min = 0, max = 4000, message = "Protective factors must be <= 4000 characters")
  override val protectiveFactors: String?,
  @field:Valid
  override val interviews: List<SyncInterviewRequest>,
) : InvestigationRequest, InterviewsRequest {
  fun findRequiredReferenceDataKeys(): Set<ReferenceDataKey> =
    interviews.flatMap { it.findRequiredReferenceDataKeys() }.toSet()

  fun requestMappings(): Set<RequestMapping> = buildSet {
    addAll(interviews.map { RequestMapping(CsipComponent.INTERVIEW, it.legacyId, it.id) })
  }
}

data class SyncInterviewRequest(
  @field:Size(min = 0, max = 100, message = "Interviewee name must be <= 100 characters")
  override val interviewee: String,
  override val interviewDate: LocalDate,
  @field:Size(min = 1, max = 12, message = "Interviewee role code must be <= 12 characters")
  override val intervieweeRoleCode: String,
  @field:Size(min = 0, max = 4000, message = "Interview text must be <= 4000 characters")
  override val interviewText: String?,
  override val legacyId: Long,
  override val id: UUID?,
) : NomisAudited(), NomisIdentifiable, InterviewRequest, LegacyIdAware {
  fun findRequiredReferenceDataKeys(): Set<ReferenceDataKey> =
    setOf(ReferenceDataKey(ReferenceDataType.INTERVIEWEE_ROLE, intervieweeRoleCode))
}

@ValidDecisionDetail
data class SyncDecisionAndActionsRequest(
  @field:Size(min = 0, max = 4000, message = "Conclusion must be <= 4000 characters")
  override val conclusion: String?,
  @field:Size(min = 1, max = 12, message = "Decision outcome code must be <= 12 characters")
  override val outcomeTypeCode: String?,
  @field:Size(min = 0, max = 12, message = "Signed off by role code must be <= 12 characters")
  override val signedOffByRoleCode: String?,
  override val date: LocalDate?,
  @field:Size(min = 0, max = 64, message = "Recorded by username must be <= 64 characters")
  override val recordedBy: String?,
  @field:Size(min = 0, max = 255, message = "Recorded by display name must be <= 255 characters")
  override val recordedByDisplayName: String?,
  @field:Size(min = 0, max = 4000, message = "Next step must be <= 4000 characters")
  override val nextSteps: String?,
  @field:Size(min = 0, max = 4000, message = "Action other must be <= 4000 characters")
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
