package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toCsipRecordEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.Companion.getOutcomeType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.CsipRecordNotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import java.util.*

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
    val decisionOutcome = getOutcomeType(request.outcomeTypeCode, referenceDataRepository)
    val decisionOutcomeSignedOffBy = if (request.outcomeSignedOffByRoleCode != null) getOutcomeType(
      request.outcomeSignedOffByRoleCode,
      referenceDataRepository,
    ) else null

    return csipRecordRepository.findByRecordUuid(recordUuid)?.let {
      it.referral?.let { referral ->
        csipRecordRepository.saveAndFlush(
          request.toCsipRecordEntity(
            referral = referral,
            decisionOutcome,
            decisionOutcomeSignedOffBy = decisionOutcomeSignedOffBy,
            decisionConclusion = request.conclusion,
            decisionOutcomeRecordedBy = context.username,
            decisionOutcomeRecordedByDisplayName = context.userDisplayName,
            decisionOutcomeDate = context.requestAt.toLocalDate(),
            nextSteps = request.nextSteps,
            actionOpenCsipAlert = request.isActionOpenCsipAlert,
            actionNonAssociationsUpdated = request.isActionNonAssociationsUpdated,
            actionObservationBook = request.isActionObservationBook,
            actionUnitOrCellMove = request.isActionUnitOrCellMove,
            actionCsraOrRsraReview = request.isActionCsraOrRsraReview,
            actionServiceReferral = request.isActionServiceReferral,
            actionSimReferral = request.isActionSimReferral,
            actionOther = request.actionOther,
            actionedAt = context.requestAt,
            source = context.source,
            activeCaseLoadId = context.activeCaseLoadId,
          ),
        ).referral()!!.decisionAndActions()!!.toModel()
      } ?: throw MissingReferralException(recordUuid)
    } ?: throw CsipRecordNotFoundException("Could not find CSIP record with UUID $recordUuid")
  }
}
