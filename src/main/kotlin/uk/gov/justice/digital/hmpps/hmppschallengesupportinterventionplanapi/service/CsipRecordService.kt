package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CsipSummaries
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CsipSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.FindCsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.createdAfter
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.createdBefore
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.matchesLogCode
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.prisonNumberIn
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.saveAndRefresh
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.verifyAllReferenceData
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord as CsipEntity

@Service
@Transactional
class CsipRecordService(
  private val referenceDataRepository: ReferenceDataRepository,
  private val csipRecordRepository: CsipRecordRepository,
) {
  fun createCsipRecord(
    prisoner: PrisonerDto,
    request: CreateCsipRecordRequest,
  ): CsipRecord {
    val context = csipRequestContext()
    val rdMap = referenceDataRepository.verifyAllReferenceData(
      ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE,
      request.referral.contributoryFactors.map { it.factorTypeCode }.toSet(),
    )
    val record = CsipEntity(prisoner.prisonerNumber, prisoner.prisonId, request.logCode)
    val referral = record.createReferral(request.referral, context, referenceDataRepository::getActiveReferenceData)
    request.referral.contributoryFactors.forEach {
      referral.addContributoryFactor(it) { type, code -> requireNotNull(rdMap[ReferenceDataKey(type, code)]) }
    }
    return csipRecordRepository.saveAndRefresh(record).toModel()
  }

  @Transactional(readOnly = true)
  fun retrieveCsipRecord(recordUuid: UUID): CsipRecord = csipRecordRepository.getCsipRecord(recordUuid).toModel()

  fun updateCsipRecord(
    recordUuid: UUID,
    request: UpdateCsipRecordRequest,
  ): CsipRecord {
    val record = csipRecordRepository.getCsipRecord(recordUuid).updateWithReferral(request) { type, code ->
      referenceDataRepository.getActiveReferenceData(type, code)
    }
    return csipRecordRepository.saveAndRefresh(record).toModel()
  }

  fun deleteCsipRecord(recordUuid: UUID): Boolean =
    csipRecordRepository.findById(recordUuid)?.also(csipRecordRepository::delete) != null

  fun findCsipRecords(request: FindCsipRequest): CsipSummaries =
    csipRecordRepository.findAll(request.toSpecification(), request.pageable).map { it.toSummary() }.asCsipSummaries()
}

private fun FindCsipRequest.toSpecification(): Specification<CsipEntity> = listOfNotNull(
  prisonNumberIn(prisonNumbers),
  logCode?.let { matchesLogCode(it) },
  from?.let { createdAfter(it) },
  to?.let { createdBefore(it) },
).reduce { spec, current -> spec.and(current) }

private fun CsipEntity.toSummary(): CsipSummary {
  val referral = requireNotNull(referral) { IllegalStateException("Referral not yet created") }
  return CsipSummary(
    id,
    prisonNumber,
    logCode,
    referral.referralDate,
    plan?.nextReviewDate(),
    referral.incidentType.toReferenceDataModel(),
    plan?.caseManager,
    status,
  )
}

private fun Page<CsipSummary>.asCsipSummaries() = CsipSummaries(
  content,
  PagedModel.PageMetadata(size.toLong(), number.toLong(), totalElements, totalPages.toLong()),
)
