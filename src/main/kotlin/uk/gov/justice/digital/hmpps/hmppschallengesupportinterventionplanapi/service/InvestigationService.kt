package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toCsipRecordEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INTERVIEWEE_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidReferenceCodeType.DOES_NOT_EXIST
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidReferenceCodeType.IS_INACTIVE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidReferenceDataCodeException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.associateByAndHandleException
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
    val intervieweeRoleMap: Map<String, ReferenceData> = request.interviews?.let { interviews ->
      val roles = referenceDataRepository.findByDomain(INTERVIEWEE_ROLE)
      interviews.associateByAndHandleException(
        { it.intervieweeRoleCode },
        { it.getInterviewRole(roles) },
        InvalidReferenceDataCodeException::combineToIllegalArgumentException,
      )
    } ?: emptyMap()

    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)

    return with(verifyExists(record.referral) { MissingReferralException(recordUuid) }) {
      csipRecordRepository.save(
        request.toCsipRecordEntity(
          referral = this,
          intervieweeRoleMap = intervieweeRoleMap,
          actionedAt = context.requestAt,
          actionedBy = context.username,
          actionedByDisplayName = context.userDisplayName,
          source = context.source,
          activeCaseLoadId = context.activeCaseLoadId,
        ),
      ).referral()!!.investigation()!!.toModel()
    }
  }

  private fun CreateInterviewRequest.getInterviewRole(roles: Collection<ReferenceData>): ReferenceData {
    return roles.find { it.code == intervieweeRoleCode }?.also {
      if (!it.isActive()) throw InvalidReferenceDataCodeException(IS_INACTIVE, INTERVIEWEE_ROLE, intervieweeRoleCode)
    } ?: throw InvalidReferenceDataCodeException(DOES_NOT_EXIST, INTERVIEWEE_ROLE, intervieweeRoleCode)
  }
}
