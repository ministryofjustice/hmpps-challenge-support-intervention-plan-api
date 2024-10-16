package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.createdAfter
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.createdBefore
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.isLikeLogCode
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.matchesPrisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.toReferenceDataModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.verifyAllReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.saveAndRefresh
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_DELETED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PublishCsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSummaries
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.PageMeta
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CsipSummaryRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord as CsipEntity

@Service
@Transactional
class CsipRecordService(
  private val referenceDataRepository: ReferenceDataRepository,
  private val personSummaryRepository: PersonSummaryRepository,
  private val personSearch: PrisonerSearchClient,
  private val csipRecordRepository: CsipRecordRepository,
) {
  @PublishCsipEvent(CSIP_CREATED)
  fun createCsipRecord(
    prisonNumber: String,
    request: CreateCsipRecordRequest,
  ): CsipRecord {
    val context = csipRequestContext()
    val rdMap = referenceDataRepository.verifyAllReferenceData(
      ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE,
      request.referral.contributoryFactors.map { it.factorTypeCode }.toSet(),
    )
    val personSummary = getPersonSummary(prisonNumber)
    val record = CsipEntity(personSummary, personSummary.prisonCode, request.logCode)
    val referral = record.createReferral(request.referral, context, referenceDataRepository::getActiveReferenceData)
    request.referral.contributoryFactors.forEach {
      referral.addContributoryFactor(it) { type, code -> requireNotNull(rdMap[ReferenceDataKey(type, code)]) }
    }
    return csipRecordRepository.saveAndRefresh(record).toModel()
  }

  @Transactional(readOnly = true)
  fun retrieveCsipRecord(recordUuid: UUID): CsipRecord = csipRecordRepository.getCsipRecord(recordUuid).toModel()

  @PublishCsipEvent(CSIP_UPDATED)
  fun updateCsipRecord(
    recordUuid: UUID,
    request: UpdateCsipRecordRequest,
  ): CsipRecord {
    val record = csipRecordRepository.getCsipRecord(recordUuid).updateWithReferral(request) { type, code ->
      referenceDataRepository.getActiveReferenceData(type, code)
    }
    return csipRecordRepository.saveAndRefresh(record).toModel()
  }

  @PublishCsipEvent(CSIP_DELETED)
  fun deleteCsipRecord(recordUuid: UUID): Boolean =
    csipRecordRepository.findById(recordUuid)?.also {
      val remaining = csipRecordRepository.countByPrisonNumber(it.prisonNumber)
      csipRecordRepository.delete(it)
      if (remaining == 1) {
        personSummaryRepository.delete(it.personSummary)
      }
    } != null

  @Transactional(readOnly = true)
  fun findCsipRecordsForPrisoner(prisonNumber: String, request: CsipSummaryRequest): CsipSummaries =
    csipRecordRepository.findAll(request.toSpecification(prisonNumber), request.pageable()).map { it.toSummary() }
      .asCsipSummaries()

  private fun getPersonSummary(prisonNumber: String): PersonSummary {
    return personSummaryRepository.findByIdOrNull(prisonNumber)
      ?: personSummaryRepository.save(
        requireNotNull(personSearch.getPrisoner(prisonNumber)) { "Prisoner number invalid" }.toPersonSummary(),
      )
  }
}

private fun CsipSummaryRequest.toSpecification(prisonNumber: String): Specification<CsipEntity> = listOfNotNull(
  matchesPrisonNumber(prisonNumber),
  logCode?.let { isLikeLogCode(it) },
  createdAtStart?.let { createdAfter(it) },
  createdAtEnd?.let { createdBefore(it) },
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
  PageMeta(totalElements),
)
