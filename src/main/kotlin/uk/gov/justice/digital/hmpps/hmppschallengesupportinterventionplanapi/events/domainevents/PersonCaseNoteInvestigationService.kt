package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PersonCaseNoteInvestigationService(
  private val caseNoteAnalysedRepository: CaseNoteAnalysedRepository,
  private val csipRecordRepository: CsipRecordRepository,
) {
  fun resolveInvestigations(prisonNumber: String): PersonCaseNoteInvestigationResolution {
    val investigationIds = caseNoteAnalysedRepository.findByPrisonerNumber(prisonNumber)
      .map { it.investigationId }
      .distinct()

    val evaluations = investigationIds.map { investigationId ->
      val record = csipRecordRepository.findById(investigationId)
      when {
        record == null -> InvestigationEvaluation(
          investigationId = investigationId,
          status = null,
          eligible = false,
          reason = "missing CSIP record",
        )

        record.status?.code == CsipStatus.INVESTIGATION_PENDING.name -> InvestigationEvaluation(
          investigationId = investigationId,
          status = record.status?.code,
          eligible = true,
        )

        else -> InvestigationEvaluation(
          investigationId = investigationId,
          status = record.status?.code ?: CsipStatus.UNKNOWN.name,
          eligible = false,
          reason = "status is not ${CsipStatus.INVESTIGATION_PENDING.name}",
        )
      }
    }

    return PersonCaseNoteInvestigationResolution(
      prisonNumber = prisonNumber,
      discoveredInvestigationIds = investigationIds,
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

