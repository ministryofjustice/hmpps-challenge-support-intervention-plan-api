package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PersonCaseNoteInvestigationService(
  private val csipRecordRepository: CsipRecordRepository,
) {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun resolveInvestigations(prisonNumber: String): PersonCaseNoteInvestigationResolution {
    val records = csipRecordRepository.findByPrisonNumber(prisonNumber)
    log.info("Loaded {} CSIP records for prison number {}: {}", records.size, prisonNumber, records.map { it.id })

    val distinctRecords = records.distinctBy { it.id }
    log.info("Distinct investigation IDs for prison number {}: {}", prisonNumber, distinctRecords.map { it.id })

    val evaluations = distinctRecords.map { record ->
      when {
        record.status?.code == CsipStatus.INVESTIGATION_PENDING.name -> InvestigationEvaluation(
          investigationId = record.id,
          status = record.status?.code,
          eligible = true,
        )

        else -> InvestigationEvaluation(
          investigationId = record.id,
          status = record.status?.code ?: CsipStatus.UNKNOWN.name,
          eligible = false,
          reason = "status is not ${CsipStatus.INVESTIGATION_PENDING.name}",
        )
      }
    }

    return PersonCaseNoteInvestigationResolution(
      prisonNumber = prisonNumber,
      discoveredInvestigationIds = distinctRecords.map { it.id },
      evaluations = evaluations,
    )
  }
}

data class PersonCaseNoteInvestigationResolution(
  val prisonNumber: String,
  val discoveredInvestigationIds: List<UUID>,
  val evaluations: List<InvestigationEvaluation>,
) {
  val eligibleInvestigationIds: List<UUID>
    get() = evaluations.filter { it.eligible }.map { it.investigationId }

  val ignoredInvestigations: Map<UUID, String>
    get() = evaluations.filterNot { it.eligible }.associate { it.investigationId to (it.reason ?: "ignored") }
}

data class InvestigationEvaluation(
  val investigationId: UUID,
  val status: String?,
  val eligible: Boolean,
  val reason: String? = null,
)
