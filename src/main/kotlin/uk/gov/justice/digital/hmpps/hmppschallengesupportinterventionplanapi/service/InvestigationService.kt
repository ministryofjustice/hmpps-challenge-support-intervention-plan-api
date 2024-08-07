package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingInvestigationException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getActiveReferenceData
import java.util.UUID

@Service
@Transactional
class InvestigationService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun upsertInvestigation(
    recordUuid: UUID,
    request: UpsertInvestigationRequest,
  ): Investigation {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val referral = verifyExists(record.referral) { MissingReferralException(recordUuid) }
    val investigation = referral.investigation
    return csipRecordRepository.save(referral.upsertInvestigation(csipRequestContext(), request))
      .referral!!.investigation!!.toModel().apply { new = investigation == null }
  }

  fun addInterview(recordUuid: UUID, request: CreateInterviewRequest): Interview {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val referral = verifyExists(record.referral) { MissingReferralException(recordUuid) }
    val investigation = verifyExists(referral.investigation) { MissingInvestigationException(recordUuid) }
    val interview = investigation.addInterview(csipRequestContext(), request) { code ->
      referenceDataRepository.getActiveReferenceData(ReferenceDataType.INTERVIEWEE_ROLE, code)
    }
    return csipRecordRepository.save(record).referral!!.investigation!!.interviews()
      .find { it.interviewUuid == interview.interviewUuid }!!.toModel()
  }

  fun createInvestigationWithInterviews(recordUuid: UUID, request: CreateInvestigationRequest): Investigation {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val referral = verifyExists(record.referral) { MissingReferralException(recordUuid) }
    verifyDoesNotExist(referral.investigation) {
      ResourceAlreadyExistException("Referral already has an investigation")
    }
    val context = csipRequestContext()
    val investigation = referral.upsertInvestigation(context, request).referral!!.investigation!!
    request.interviews.forEach {
      investigation.addInterview(context, it) { code ->
        referenceDataRepository.getActiveReferenceData(ReferenceDataType.INTERVIEWEE_ROLE, code)
      }
    }
    return csipRecordRepository.save(record).referral!!.investigation!!.toModel()
  }
}
