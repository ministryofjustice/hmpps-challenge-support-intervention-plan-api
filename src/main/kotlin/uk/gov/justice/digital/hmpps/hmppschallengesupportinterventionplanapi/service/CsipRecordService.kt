package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
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
    val incidentType = referenceDataRepository.findByDomainAndCode(INCIDENT_TYPE, request.referral.incidentTypeCode)
    val incidentLocation =
      referenceDataRepository.findByDomainAndCode(INCIDENT_LOCATION, request.referral.incidentLocationCode)
    val referrerAreaOfWork = referenceDataRepository.findByDomainAndCode(AREA_OF_WORK, request.referral.refererAreaCode)
    val incidentInvolvement =
      referenceDataRepository.findByDomainAndCode(INCIDENT_INVOLVEMENT, request.referral.incidentInvolvementCode)
    require(incidentType != null) { "Incident type code ${request.referral.incidentTypeCode} could not be found" }
    require(incidentLocation != null) { "Incident location code ${request.referral.incidentLocationCode} could not be found" }
    require(referrerAreaOfWork != null) { "Area of work code ${request.referral.refererAreaCode} could not be found" }
    require(incidentInvolvement != null) { "Incident involvement code ${request.referral.incidentInvolvementCode} could not be found" }
    val contributoryFactors = request.referral.contributoryFactors.map {
      val factorType = referenceDataRepository.findByDomainAndCode(CONTRIBUTORY_FACTOR_TYPE, it.factorTypeCode)
      require(factorType != null) { "Contributory factor type code ${it.factorTypeCode} could not be found" }
      factorType
    }
    val record = request.toCsipRecord(prisonNumber, requestContext).create(
      request,
      requestContext,
      incidentType,
      incidentLocation,
      referrerAreaOfWork,
      incidentInvolvement,
      contributoryFactors,
    )
    return csipRecordRepository.saveAndFlush(record).toModel()
  }
}
