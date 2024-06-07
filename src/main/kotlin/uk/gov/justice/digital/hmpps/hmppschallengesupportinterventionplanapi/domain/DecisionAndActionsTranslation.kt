package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateDecisionAndActionsRequest
import java.time.LocalDate
import java.time.LocalDateTime

fun DecisionAndActions.toModel() =
  uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions(
    decisionConclusion,
    decisionOutcome.toReferenceDataModel(),
    decisionOutcomeSignedOffBy?.toReferenceDataModel(),
    decisionOutcomeRecordedBy,
    decisionOutcomeRecordedByDisplayName,
    decisionOutcomeDate,
    nextSteps,
    actionOpenCsipAlert,
    actionNonAssociationsUpdated,
    actionObservationBook,
    actionUnitOrCellMove,
    actionCsraOrRsraReview,
    actionServiceReferral,
    actionSimReferral,
    actionOther,
  )

fun CreateDecisionAndActionsRequest.toCsipRecordEntity(
  referral: Referral,
  decisionOutcome: ReferenceData,
  decisionOutcomeSignedOffBy: ReferenceData?,
  decisionConclusion: String?,
  decisionOutcomeRecordedBy: String,
  decisionOutcomeRecordedByDisplayName: String,
  decisionOutcomeDate: LocalDate?,
  nextSteps: String?,
  actionOpenCsipAlert: Boolean?,
  actionNonAssociationsUpdated: Boolean?,
  actionObservationBook: Boolean?,
  actionUnitOrCellMove: Boolean?,
  actionCsraOrRsraReview: Boolean?,
  actionServiceReferral: Boolean?,
  actionSimReferral: Boolean?,
  actionOther: String?,
  actionedAt: LocalDateTime = LocalDateTime.now(),
  source: Source,
  activeCaseLoadId: String?,
  reason: Reason = Reason.USER,
) = referral.createDecisionAndActions(
  decisionOutcome,
  decisionOutcomeSignedOffBy,
  decisionConclusion,
  decisionOutcomeRecordedBy,
  decisionOutcomeRecordedByDisplayName,
  decisionOutcomeDate,
  nextSteps,
  actionOther,
  actionedAt,
  source,
  activeCaseLoadId,
  reason,
  actionOpenCsipAlert,
  actionNonAssociationsUpdated,
  actionObservationBook,
  actionUnitOrCellMove,
  actionCsraOrRsraReview,
  actionServiceReferral,
  actionSimReferral,
)
