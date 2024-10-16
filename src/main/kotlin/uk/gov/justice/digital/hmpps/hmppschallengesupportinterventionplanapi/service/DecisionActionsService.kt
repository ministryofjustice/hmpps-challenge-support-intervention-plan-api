package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PublishCsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpsertDecisionAndActionsRequest
import java.util.UUID

@Service
@Transactional
class DecisionActionsService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  @PublishCsipEvent(CSIP_UPDATED)
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
