package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidReferenceCodeType.DOES_NOT_EXIST
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidReferenceCodeType.IS_INACTIVE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidReferenceDataCodeException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.associateByAndHandleException

@Service
@Transactional
class CsipRecordService(
  val referenceDataRepository: ReferenceDataRepository,
  val csipRecordRepository: CsipRecordRepository,
  val prisonerSearchClient: PrisonerSearchClient,
) {
  fun createCsipRecord(
    request: CreateCsipRecordRequest,
    prisonNumber: String,
    requestContext: CsipRequestContext,
  ): CsipRecord {
    val prisoner = prisonerSearchClient.getPrisoner(prisonNumber)
    require(prisoner != null) { "Prisoner with prison number $prisonNumber could not be found" }
    val incidentType = referenceDataRepository.getReferenceData(INCIDENT_TYPE, request.referral.incidentTypeCode)
    val incidentLocation =
      referenceDataRepository.getReferenceData(INCIDENT_LOCATION, request.referral.incidentLocationCode)
    val referrerAreaOfWork = referenceDataRepository.getReferenceData(AREA_OF_WORK, request.referral.refererAreaCode)
    val incidentInvolvement = if (request.referral.incidentInvolvementCode != null)
      referenceDataRepository.getReferenceData(INCIDENT_INVOLVEMENT, request.referral.incidentInvolvementCode) else null

    val contributoryFactors = request.referral.contributoryFactors.let { factors ->
      val factorTypes = referenceDataRepository.findByDomain(CONTRIBUTORY_FACTOR_TYPE)
      factors.associateByAndHandleException(
        { it.factorTypeCode },
        { it.getFactorType(factorTypes) },
        InvalidReferenceDataCodeException::combineToIllegalArgumentException,
      )
    }

    val record = request.toCsipRecord(prisonNumber, requestContext).create(
      createCsipRecordRequest = request,
      csipRequestContext = requestContext,
      incidentType = incidentType,
      incidentLocation = incidentLocation,
      referrerAreaOfWork = referrerAreaOfWork,
      incidentInvolvement = incidentInvolvement,
      contributoryFactors = contributoryFactors,
    )
    return csipRecordRepository.saveAndFlush(record).toModel()
  }

  private fun ReferenceDataRepository.getReferenceData(type: ReferenceDataType, code: String) =
    findByDomainAndCode(type, code)?.also {
      require(it.isActive()) { "$type code '$code' is inactive" }
    } ?: throw IllegalArgumentException("$type code '$code' does not exist")

  private fun CreateContributoryFactorRequest.getFactorType(roles: Collection<ReferenceData>): ReferenceData {
    return roles.find { it.code == factorTypeCode }?.also {
      if (!it.isActive()) throw InvalidReferenceDataCodeException(IS_INACTIVE, CONTRIBUTORY_FACTOR_TYPE, factorTypeCode)
    } ?: throw InvalidReferenceDataCodeException(DOES_NOT_EXIST, CONTRIBUTORY_FACTOR_TYPE, factorTypeCode)
  }
}
