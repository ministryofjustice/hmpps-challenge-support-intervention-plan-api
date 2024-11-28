package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.internal

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.matchesPrisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_MOVED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidInputException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MultipleInvalidException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.PersonSummaryService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.MoveCsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.PersonSummaryRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.RequestMapping
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.ResponseMapping
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncCsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncResponse
import java.time.LocalDateTime.now
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord as CsipModel

@Service
@Transactional
class SyncCsipRecord(
  val referenceDaRepository: ReferenceDataRepository,
  val csipRepository: CsipRecordRepository,
  val personSummaryService: PersonSummaryService,
  val referralSync: SyncReferral,
  val planSync: SyncPlan,
  val eventPublisher: ApplicationEventPublisher,
  val telemetry: TelemetryClient,
) {
  fun sync(request: SyncCsipRequest): SyncResponse {
    val rdMap = validatedReferenceData(request.findRequiredReferenceDataKeys().toSet())
    val rdSupplier: (ReferenceDataType, String) -> ReferenceData = { type, code ->
      requireNotNull(rdMap[ReferenceDataKey(type, code)])
    }

    val csip: CsipRecord = findCsipRecord(request.id, request.legacyId)?.update(request)
      ?: request.toCsipRecord(request.toPersonSummary())
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

  fun move(request: MoveCsipRequest) {
    val toMove = csipRepository.findByPrisonNumberAndIdIn(request.fromPrisonNumber, request.recordUuids)
    if (toMove.isNotEmpty()) {
      val personSummary = personSummaryService.getPersonSummaryByPrisonNumber(request.toPrisonNumber)
      val remaining = csipRepository.countByPrisonNumber(request.fromPrisonNumber)
      val moved = toMove.map { csip ->
        csip.moveTo(personSummary).also {
          eventPublisher.publishEvent(CsipEvent(CSIP_MOVED, it.prisonNumber, it.id, now(), request.fromPrisonNumber))
        }
      }
      if (remaining == moved.size) {
        personSummaryService.removePersonSummaryByPrisonNumber(request.fromPrisonNumber)
      }
    }
  }

  fun deleteCsipRecord(id: UUID) {
    csipRepository.findById(id)?.also {
      val remaining = csipRepository.countByPrisonNumber(it.prisonNumber)
      csipRepository.delete(it)
      if (remaining == 1) {
        personSummaryService.removePersonSummaryByPrisonNumber(it.personSummary.prisonNumber)
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

  private fun SyncCsipRequest.toPersonSummary() =
    personSummary?.toPersonSummary(prisonNumber)?.let(personSummaryService::savePersonSummary)
      ?: personSummaryService.getPersonSummaryByPrisonNumber(prisonNumber)

  private fun PersonSummaryRequest.toPersonSummary(prisonNumber: String) =
    PersonSummary(
      prisonNumber,
      firstName,
      lastName,
      status,
      restrictedPatient ?: false,
      prisonCode,
      cellLocation,
      supportingPrisonCode,
    )

  private fun SyncCsipRequest.toCsipRecord(personSummary: PersonSummary): CsipRecord =
    CsipRecord(personSummary, prisonCodeWhenRecorded, logCode, legacyId)

  fun findFor(prisonNumber: String): List<CsipModel> =
    csipRepository.findAll(matchesPrisonNumber(prisonNumber)).map { it.toModel() }
}
