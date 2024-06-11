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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository

@Service
class CsipRecordService(
  val referenceDataRepository: ReferenceDataRepository,
  val csipRecordRepository: CsipRecordRepository,
  val prisonerSearchClient: PrisonerSearchClient,
) {

  @Transactional
  fun createCsipRecord(
    request: CreateCsipRecordRequest,
    prisonNumber: String,
    requestContext: CsipRequestContext,
  ): CsipRecord {
    val prisoner = prisonerSearchClient.getPrisoner(prisonNumber)
    require(prisoner != null) { "Prisoner with prison number $prisonNumber could not be found" }
    val incidentType = getReferenceData(INCIDENT_TYPE, request.referral.incidentTypeCode)
    val incidentLocation = getReferenceData(INCIDENT_LOCATION, request.referral.incidentLocationCode)
    val referrerAreaOfWork = getReferenceData(AREA_OF_WORK, request.referral.refererAreaCode)
    val incidentInvolvement = getReferenceData(INCIDENT_INVOLVEMENT, request.referral.incidentInvolvementCode)

    val contributoryFactors: Map<String, ReferenceData> = request.referral.contributoryFactors.let { factors ->
      val factorTypes = referenceDataRepository.findByDomain(CONTRIBUTORY_FACTOR_TYPE)
      factors.associateBy({ it.factorTypeCode }, { it.getFactorType(factorTypes) })
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

  private fun getReferenceData(type: ReferenceDataType, code: String) =
    referenceDataRepository.findByDomainAndCode(type, code)?.also {
      require(it.isActive()) { "$type code '$code' is inactive" }
    } ?: throw IllegalArgumentException("$type code '$code' does not exist")

  private fun CreateContributoryFactorRequest.getFactorType(roles: Collection<ReferenceData>): ReferenceData {
    return roles.find { it.code.equals(factorTypeCode) }?.also {
      require(it.isActive()) { "CONTRIBUTORY_FACTOR_TYPE code '$factorTypeCode' is inactive" }
    } ?: throw IllegalArgumentException("CONTRIBUTORY_FACTOR_TYPE code '$factorTypeCode' does not exist")
  }
}
