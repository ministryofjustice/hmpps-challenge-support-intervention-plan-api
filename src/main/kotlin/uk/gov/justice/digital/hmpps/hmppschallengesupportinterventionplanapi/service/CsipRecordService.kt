package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidInputException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotActiveException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verify
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.verifyAllReferenceData
import java.util.UUID

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
    val prisoner = requireNotNull(prisonerSearchClient.getPrisoner(prisonNumber)) { "Prisoner number invalid" }
    val referral = request.referral
    if (requestContext.source != Source.NOMIS) {
      require(referral.contributoryFactors.isNotEmpty()) { "A referral must have >=1 contributory factor(s)." }
    }

    val incidentType = referenceDataRepository.getReferenceData(INCIDENT_TYPE, referral.incidentTypeCode)
    val incidentLocation = referenceDataRepository.getReferenceData(INCIDENT_LOCATION, referral.incidentLocationCode)
    val referrerAreaOfWork = referenceDataRepository.getReferenceData(AREA_OF_WORK, referral.refererAreaCode)
    val incidentInvolvement = referral.incidentInvolvementCode?.let {
      referenceDataRepository.getReferenceData(INCIDENT_INVOLVEMENT, it)
    }

    val factorTypeCodes = referral.contributoryFactors.map { it.factorTypeCode }.toSet()
    val contributoryFactors = referenceDataRepository.verifyAllReferenceData(CONTRIBUTORY_FACTOR_TYPE, factorTypeCodes)

    val record = request.toCsipRecord(prisonNumber, prisoner.prisonId, requestContext).create(
      createCsipRecordRequest = request,
      csipRequestContext = requestContext,
      incidentType = incidentType,
      incidentLocation = incidentLocation,
      referrerAreaOfWork = referrerAreaOfWork,
      incidentInvolvement = incidentInvolvement,
      contributoryFactors = contributoryFactors,
    )
    return csipRecordRepository.save(record).toModel()
  }

  private fun ReferenceDataRepository.getReferenceData(type: ReferenceDataType, code: String) =
    verifyExists(findByDomainAndCode(type, code)) {
      InvalidInputException(type.name, code)
    }.also {
      verify(it.isActive()) { NotActiveException(type.name, code) }
    }

  fun retrieveCsipRecord(recordUuid: UUID): CsipRecord = csipRecordRepository.getCsipRecord(recordUuid).toModel()
}
