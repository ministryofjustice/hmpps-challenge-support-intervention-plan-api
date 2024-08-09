package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord as CsipEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.saveAndRefresh
import java.util.UUID

@Service
@Transactional
class CsipRecordService(
  private val referenceDataRepository: ReferenceDataRepository,
  private val csipRecordRepository: CsipRecordRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
) {
  fun createCsipRecord(
    prisonNumber: String,
    request: CreateCsipRecordRequest,
  ): CsipRecord {
    val context = csipRequestContext()
    val prisoner = requireNotNull(prisonerSearchClient.getPrisoner(prisonNumber)) { "Prisoner number invalid" }
    val referral = request.referral
    if (context.source != Source.NOMIS) {
      require(referral.contributoryFactors.isNotEmpty()) { "A referral must have at least one contributory factor." }
    }

    val record = CsipEntity(prisonNumber, prisoner.prisonId, request.logCode)
      .create(request, context, referenceDataRepository)
    return csipRecordRepository.saveAndRefresh(record).toModel()
  }

  fun retrieveCsipRecord(recordUuid: UUID): CsipRecord = csipRecordRepository.getCsipRecord(recordUuid).toModel()

  fun updateCsipRecord(
    recordUuid: UUID,
    request: UpdateCsipRecordRequest,
  ): CsipRecord {
    val record = csipRecordRepository.getCsipRecord(recordUuid)
      .update(csipRequestContext(), request) { type, code ->
        referenceDataRepository.getActiveReferenceData(type, code)
      }
    return csipRecordRepository.saveAndRefresh(record).toModel()
  }

  fun deleteCsipRecord(recordUuid: UUID): Boolean =
    csipRecordRepository.findByRecordUuid(recordUuid)
      ?.delete(csipRequestContext())?.also(csipRecordRepository::delete) != null
}
