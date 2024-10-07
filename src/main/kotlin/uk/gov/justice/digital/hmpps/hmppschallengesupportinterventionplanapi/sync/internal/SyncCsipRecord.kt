package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.internal

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidInputException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MultipleInvalidException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.PersonSummaryRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.RequestMapping
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.ResponseMapping
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncCsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncResponse
import java.util.UUID

@Service
@Transactional
class SyncCsipRecord(
  val referenceDaRepository: ReferenceDataRepository,
  val csipRepository: CsipRecordRepository,
  val personSummaryRepository: PersonSummaryRepository,
  val personSearch: PrisonerSearchClient,
  val referralSync: SyncReferral,
  val planSync: SyncPlan,
  val telemetry: TelemetryClient,
) {
  fun sync(request: SyncCsipRequest): SyncResponse {
    val rdMap = validatedReferenceData(request.findRequiredReferenceDataKeys().toSet())
    val rdSupplier: (ReferenceDataType, String) -> ReferenceData = { type, code ->
      requireNotNull(rdMap[ReferenceDataKey(type, code)])
    }
    val personSummary = personSummaryRepository.findByIdOrNull(request.prisonNumber)
    val csip: CsipRecord = findCsipRecord(request.id, request.legacyId)?.update(request)?.withAuditInfo(request)
      ?: request.toCsipRecord(personSummary ?: request.toPersonSummary())
    val referralMappings = request.referral?.let {
      val referral = csip.referral?.update(it, rdSupplier) ?: csip.createReferral(it, csipRequestContext(), rdSupplier)
      referralSync.sync(referral, it, rdSupplier)
    } ?: emptySet()
    val planMappings = request.plan?.let {
      val plan = csip.plan?.update(it) ?: csip.createPlan(it)
      planSync.sync(plan, it)
    } ?: emptySet()

    csipRepository.save(csip)

    val requestMappings = request.requestMappings()
    val responseMappings = referralMappings + planMappings +
      ResponseMapping(CsipComponent.RECORD, request.legacyId, csip.id)

    return SyncResponse(
      responseMappings.filter { RequestMapping(it.component, it.id, it.uuid) !in requestMappings }
        .toSet(),
    )
  }

  fun deleteCsipRecord(id: UUID) {
    csipRepository.findById(id)?.also {
      val remaining = csipRepository.countByPrisonNumber(it.prisonNumber)
      csipRepository.delete(it)
      if (remaining == 1) {
        personSummaryRepository.delete(it.personSummary)
      }
    }
  }

  private fun findCsipRecord(uuid: UUID?, legacyId: Long): CsipRecord? =
    uuid?.let { csipRepository.getCsipRecord(it) } ?: csipRepository.findByLegacyId(legacyId)?.also {
      telemetry.trackEvent("CsipLegacyLookup", mapOf("legacyId" to legacyId.toString()), mapOf())
    }

  private fun validatedReferenceData(keys: Set<ReferenceDataKey>): Map<ReferenceDataKey, ReferenceData> {
    val rd = referenceDaRepository.findByKeyIn(keys).associateBy { it.key }
    val missing = keys.subtract(rd.keys)
    return when (missing.size) {
      0 -> rd
      1 -> throw InvalidInputException(missing.first().domain.name, missing.first().code)
      else -> {
        val lines = missing.groupBy { it.domain }
          .map { e -> "${e.key}:${e.value.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.code }}" }
        throw MultipleInvalidException(
          "Reference Data",
          lines.joinToString(separator = ", ", prefix = "{ ", postfix = " }"),
        )
      }
    }
  }

  private fun SyncCsipRequest.toPersonSummary() = (
    personSummary?.toPersonSummary(prisonNumber)
      ?: personSearch.getPrisoner(prisonNumber)?.toPersonSummary()
    )?.also(personSummaryRepository::save)
    ?: throw NotFoundException("Person", prisonNumber)

  private fun PersonSummaryRequest.toPersonSummary(prisonNumber: String) =
    PersonSummary(prisonNumber, firstName, lastName, status, prisonCode, cellLocation)

  private fun SyncCsipRequest.toCsipRecord(personSummary: PersonSummary): CsipRecord =
    CsipRecord(personSummary, prisonCodeWhenRecorded, logCode, legacyId).withAuditInfo(this)
}
