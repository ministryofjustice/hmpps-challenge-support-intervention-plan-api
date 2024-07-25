package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toCsipRecordEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INTERVIEWEE_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.verifyAllReferenceData
import java.util.UUID

@Service
@Transactional
class InvestigationService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun createInvestigation(
    recordUuid: UUID,
    request: CreateInvestigationRequest,
    context: CsipRequestContext,
  ): Investigation {
    val roleCodes = request.interviews?.map { it.intervieweeRoleCode }?.toSet() ?: emptySet()
    val intervieweeRoleMap = referenceDataRepository.verifyAllReferenceData(INTERVIEWEE_ROLE, roleCodes)

    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)

    return with(verifyExists(record.referral) { MissingReferralException(recordUuid) }) {
      csipRecordRepository.save(
        request.toCsipRecordEntity(
          context = context,
          referral = this,
          intervieweeRoleMap = intervieweeRoleMap,
          activeCaseLoadId = context.activeCaseLoadId,
        ),
      ).referral()!!.investigation()!!.toModel()
    }
  }
}
