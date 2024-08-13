package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getActiveReferenceData
import java.util.UUID

@Service
@Transactional
class DecisionActionsService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun upsertDecisionAndActionsRequest(
    recordUuid: UUID,
    request: UpsertDecisionAndActionsRequest,
  ): DecisionAndActions {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val referral = verifyExists(record.referral) { MissingReferralException(recordUuid) }
    val decisionAndActions = referral.decisionAndActions
    return referral.upsertDecisionAndActions(request = request) { type, code ->
      referenceDataRepository.getActiveReferenceData(type, code)
    }.toModel().apply { new = decisionAndActions == null }
  }
}
