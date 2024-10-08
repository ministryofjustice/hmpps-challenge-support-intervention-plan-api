package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PublishCsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingInvestigationException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.InterviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getInterview
import java.util.UUID

@Service
@Transactional
class InvestigationService(
  private val csipRecordRepository: CsipRecordRepository,
  private val interviewRepository: InterviewRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  @PublishCsipEvent(CSIP_UPDATED)
  fun createInvestigationWithInterviews(recordUuid: UUID, request: CreateInvestigationRequest): Investigation {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val referral = verifyExists(record.referral) { MissingReferralException(recordUuid) }
    val investigation = referral.createInvestigation(request)
    request.interviews.forEach {
      investigation.addInterview(it, referenceDataRepository::getActiveReferenceData)
    }
    return investigation.toModel()
  }

  @PublishCsipEvent(CSIP_UPDATED)
  fun updateInvestigation(
    recordUuid: UUID,
    request: UpdateInvestigationRequest,
  ): Investigation {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val referral = verifyExists(record.referral) { MissingReferralException(recordUuid) }
    val investigation = verifyExists(referral.investigation) { MissingInvestigationException(recordUuid) }
    return investigation.update(request).toModel()
  }

  @PublishCsipEvent(CSIP_UPDATED)
  fun addInterview(recordUuid: UUID, request: CreateInterviewRequest): Interview {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val referral = verifyExists(record.referral) { MissingReferralException(recordUuid) }
    val investigation = verifyExists(referral.investigation) { MissingInvestigationException(recordUuid) }
    return investigation.addInterview(request, referenceDataRepository::getActiveReferenceData).toModel()
  }

  @PublishCsipEvent(CSIP_UPDATED)
  fun updateInterview(interviewUuid: UUID, request: UpdateInterviewRequest): Interview =
    interviewRepository.getInterview(interviewUuid).update(request, referenceDataRepository::getActiveReferenceData)
      .toModel()
}
