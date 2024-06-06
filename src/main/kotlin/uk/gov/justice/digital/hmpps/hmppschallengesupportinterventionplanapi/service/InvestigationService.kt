package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toCsipRecordEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.CsipRecordNotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
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
      val roles = referenceDataRepository.findByDomain(ReferenceDataType.INTERVIEWEE_ROLE)
      interviews.associateBy({ it.intervieweeRoleCode }, { it.getInterviewRole(roles) })
    } ?: emptyMap()

    return csipRecordRepository.findByRecordUuid(recordUuid)?.let {
      it.referral?.let { referral ->
        csipRecordRepository.saveAndFlush(
          request.toCsipRecordEntity(
            referral = referral,
            intervieweeRoleMap = intervieweeRoleMap,
            actionedAt = context.requestAt,
            actionedBy = context.username,
            actionedByDisplayName = context.userDisplayName,
            source = context.source,
            activeCaseLoadId = context.activeCaseLoadId,
          ),
        ).referral()!!.investigation()!!.toModel()
      } ?: throw MissingReferralException(recordUuid)
    } ?: throw CsipRecordNotFoundException("Could not find CSIP record with UUID $recordUuid")
  }

  private fun CreateInterviewRequest.getInterviewRole(roles: Collection<ReferenceData>): ReferenceData {
    return roles.find { it.code.equals(intervieweeRoleCode) }?.also {
      require(it.isActive()) { "INTERVIEWEE_ROLE code '$intervieweeRoleCode' is inactive" }
    } ?: throw IllegalArgumentException("INTERVIEWEE_ROLE code '$intervieweeRoleCode' does not exist")
  }
}
