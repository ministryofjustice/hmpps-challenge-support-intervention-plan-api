package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getReferenceData
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord as CsipEntity

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
  ): CsipRecord {
    val context = csipRequestContext()
    val prisoner = requireNotNull(prisonerSearchClient.getPrisoner(prisonNumber)) { "Prisoner number invalid" }
    val referral = request.referral
    if (context.source != Source.NOMIS) {
      require(referral.contributoryFactors.isNotEmpty()) { "A referral must have at least one contributory factor." }
    }

    val record = CsipEntity(prisonNumber, prisoner.prisonId, request.logCode)
      .create(request, context, referenceDataRepository)
    return csipRecordRepository.save(record).toModel()
  }

  fun retrieveCsipRecord(recordUuid: UUID): CsipRecord = csipRecordRepository.getCsipRecord(recordUuid).toModel()

  fun updateCsipRecord(
    context: CsipRequestContext,
    recordUuid: UUID,
    request: UpdateCsipRecordRequest,
  ): CsipRecord {
    val record = csipRecordRepository.getCsipRecord(recordUuid)
      .update(context, request) { type, code -> referenceDataRepository.getReferenceData(type, code) }
    return csipRecordRepository.save(record).toModel()
  }
}
