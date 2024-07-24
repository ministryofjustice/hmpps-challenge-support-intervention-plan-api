package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getOutcomeType
import java.util.UUID

@Service
@Transactional
class DecisionActionsService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun createDecisionAndActionsRequest(
    recordUuid: UUID,
    request: CreateDecisionAndActionsRequest,
    context: CsipRequestContext,
  ): DecisionAndActions {
    val decisionOutcome = referenceDataRepository.getOutcomeType(request.outcomeTypeCode)
    val decisionOutcomeSignedOffBy =
      request.outcomeSignedOffByRoleCode?.let { referenceDataRepository.getOutcomeType(it) }

    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    return with(verifyExists(record.referral) { MissingReferralException(recordUuid) }) {
      csipRecordRepository.save(
        createDecisionAndActions(
          context = context,
          decisionOutcome = decisionOutcome,
          decisionOutcomeSignedOffBy = decisionOutcomeSignedOffBy,
          decisionConclusion = request.conclusion,
          nextSteps = request.nextSteps,
          actionOther = request.actionOther,
          actionedAt = context.requestAt,
          source = context.source,
          activeCaseLoadId = context.activeCaseLoadId,
          actionOpenCsipAlert = request.isActionOpenCsipAlert,
          actionNonAssociationsUpdated = request.isActionNonAssociationsUpdated,
          actionObservationBook = request.isActionObservationBook,
          actionUnitOrCellMove = request.isActionUnitOrCellMove,
          actionCsraOrRsraReview = request.isActionCsraOrRsraReview,
          actionServiceReferral = request.isActionServiceReferral,
          actionSimReferral = request.isActionSimReferral,
        ),
      ).referral()!!.decisionAndActions()!!.toModel()
    }
  }
}
