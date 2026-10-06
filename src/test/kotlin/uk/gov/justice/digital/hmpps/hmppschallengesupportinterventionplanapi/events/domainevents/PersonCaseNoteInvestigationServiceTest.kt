package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import java.util.UUID

class PersonCaseNoteInvestigationServiceTest {
  private val caseNoteAnalysedRepository = mock<CaseNoteAnalysedRepository>()
  private val csipRecordRepository = mock<CsipRecordRepository>()
  private val service = PersonCaseNoteInvestigationService(caseNoteAnalysedRepository, csipRecordRepository)

  @Test
  fun `no analysed rows found returns no investigations`() {
    whenever(caseNoteAnalysedRepository.findByPrisonerNumber("A1234BC")).thenReturn(emptyList())

    val result = service.resolveInvestigations("A1234BC")

    assertThat(result.discoveredInvestigationIds).isEmpty()
    assertThat(result.eligibleInvestigationIds).isEmpty()
    assertThat(result.ignoredInvestigations).isEmpty()
    verify(csipRecordRepository, never()).findById(any<UUID>())
  }

  @Test
  fun `investigation filtered because status is not investigation pending`() {
    val investigationId = UUID.randomUUID()
    val csipRecord = csipRecordWithStatus(CsipStatus.AWAITING_DECISION)
    whenever(caseNoteAnalysedRepository.findByPrisonerNumber("A1234BC"))
      .thenReturn(listOf(analysedRow(investigationId = investigationId)))
    whenever(csipRecordRepository.findById(investigationId)).thenReturn(csipRecord)

    val result = service.resolveInvestigations("A1234BC")

    assertThat(result.discoveredInvestigationIds).containsExactly(investigationId)
    assertThat(result.eligibleInvestigationIds).isEmpty()
    assertThat(result.ignoredInvestigations).containsEntry(investigationId, "status is not INVESTIGATION_PENDING")
  }

  @Test
  fun `investigation retained because status is investigation pending`() {
    val investigationId = UUID.randomUUID()
    val csipRecord = csipRecordWithStatus(CsipStatus.INVESTIGATION_PENDING)
    whenever(caseNoteAnalysedRepository.findByPrisonerNumber("A1234BC"))
      .thenReturn(listOf(analysedRow(investigationId = investigationId)))
    whenever(csipRecordRepository.findById(investigationId)).thenReturn(csipRecord)

    val result = service.resolveInvestigations("A1234BC")

    assertThat(result.discoveredInvestigationIds).containsExactly(investigationId)
    assertThat(result.eligibleInvestigationIds).containsExactly(investigationId)
    assertThat(result.ignoredInvestigations).isEmpty()
  }

  @Test
  fun `duplicate analysed rows produce only one investigation`() {
    val investigationId = UUID.randomUUID()
    val csipRecord = csipRecordWithStatus(CsipStatus.INVESTIGATION_PENDING)
    whenever(caseNoteAnalysedRepository.findByPrisonerNumber("A1234BC"))
      .thenReturn(
        listOf(
          analysedRow(investigationId = investigationId, caseNoteId = UUID.randomUUID()),
          analysedRow(investigationId = investigationId, caseNoteId = UUID.randomUUID()),
        ),
      )
    whenever(csipRecordRepository.findById(investigationId)).thenReturn(csipRecord)

    val result = service.resolveInvestigations("A1234BC")

    assertThat(result.discoveredInvestigationIds).containsExactly(investigationId)
    assertThat(result.eligibleInvestigationIds).containsExactly(investigationId)
    verify(csipRecordRepository, times(1)).findById(investigationId)
  }

  @Test
  fun `mixed statuses produce correct eligible investigation list`() {
    val eligibleId = UUID.randomUUID()
    val ignoredStatusId = UUID.randomUUID()
    val missingRecordId = UUID.randomUUID()
    val eligibleRecord = csipRecordWithStatus(CsipStatus.INVESTIGATION_PENDING)
    val ignoredStatusRecord = csipRecordWithStatus(CsipStatus.CSIP_OPEN)
    whenever(caseNoteAnalysedRepository.findByPrisonerNumber("A1234BC"))
      .thenReturn(
        listOf(
          analysedRow(investigationId = eligibleId),
          analysedRow(investigationId = ignoredStatusId),
          analysedRow(investigationId = missingRecordId),
        ),
      )
    whenever(csipRecordRepository.findById(eligibleId)).thenReturn(eligibleRecord)
    whenever(csipRecordRepository.findById(ignoredStatusId)).thenReturn(ignoredStatusRecord)
    whenever(csipRecordRepository.findById(missingRecordId)).thenReturn(null)

    val result = service.resolveInvestigations("A1234BC")

    assertThat(result.discoveredInvestigationIds).containsExactly(eligibleId, ignoredStatusId, missingRecordId)
    assertThat(result.eligibleInvestigationIds).containsExactly(eligibleId)
    assertThat(result.ignoredInvestigations)
      .containsEntry(ignoredStatusId, "status is not INVESTIGATION_PENDING")
      .containsEntry(missingRecordId, "missing CSIP record")
  }

  private fun analysedRow(
    investigationId: UUID,
    caseNoteId: UUID = UUID.randomUUID(),
    prisonerNumber: String = "A1234BC",
  ) = CaseNoteAnalysed(
    requestId = UUID.randomUUID(),
    investigationId = investigationId,
    prisonerNumber = prisonerNumber,
    caseNoteId = caseNoteId,
    promptKey = "case-note-analysis",
    promptVersion = 1,
    usualBehaviourRelevancy = 1,
    risksAndTriggersRelevancy = 1,
    protectiveFactorsRelevancy = 1,
  )

  private fun csipRecordWithStatus(status: CsipStatus): CsipRecord {
    val statusReferenceData = ReferenceData(
      key = ReferenceDataKey(ReferenceDataType.STATUS, status.name),
      description = status.name,
      listSequence = 1,
      deactivatedAt = null,
      id = 1,
    )
    val record = mock<CsipRecord>()
    whenever(record.status).thenReturn(statusReferenceData)
    return record
  }
}



