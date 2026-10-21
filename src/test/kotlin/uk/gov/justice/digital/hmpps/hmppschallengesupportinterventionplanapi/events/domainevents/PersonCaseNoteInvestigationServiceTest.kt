package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import java.util.UUID

class PersonCaseNoteInvestigationServiceTest {
  private val csipRecordRepository = mock<CsipRecordRepository>()
  private val service = PersonCaseNoteInvestigationService(csipRecordRepository)

  @Test
  fun `no CSIP records found returns no investigations`() {
    whenever(csipRecordRepository.findByPrisonNumber("A1234BC")).thenReturn(emptyList())

    val result = service.resolveInvestigations("A1234BC")

    assertThat(result.discoveredInvestigationIds).isEmpty()
    assertThat(result.eligibleInvestigationIds).isEmpty()
    assertThat(result.ignoredInvestigations).isEmpty()
    verify(csipRecordRepository, never()).findById(any<UUID>())
  }

  @Test
  fun `investigation filtered because status is not investigation pending`() {
    val investigationId = UUID.randomUUID()
    val csipRecord = csipRecordWithStatus(investigationId, CsipStatus.AWAITING_DECISION)
    whenever(csipRecordRepository.findByPrisonNumber("A1234BC")).thenReturn(listOf(csipRecord))

    val result = service.resolveInvestigations("A1234BC")

    assertThat(result.discoveredInvestigationIds).containsExactly(investigationId)
    assertThat(result.eligibleInvestigationIds).isEmpty()
    assertThat(result.ignoredInvestigations).containsEntry(investigationId, "status is not INVESTIGATION_PENDING")
  }

  @Test
  fun `investigation retained because status is investigation pending`() {
    val investigationId = UUID.randomUUID()
    val csipRecord = csipRecordWithStatus(investigationId, CsipStatus.INVESTIGATION_PENDING)
    whenever(csipRecordRepository.findByPrisonNumber("A1234BC")).thenReturn(listOf(csipRecord))

    val result = service.resolveInvestigations("A1234BC")

    assertThat(result.discoveredInvestigationIds).containsExactly(investigationId)
    assertThat(result.eligibleInvestigationIds).containsExactly(investigationId)
    assertThat(result.ignoredInvestigations).isEmpty()
  }

  @Test
  fun `duplicate records produce only one investigation`() {
    val investigationId = UUID.randomUUID()
    val csipRecord = csipRecordWithStatus(investigationId, CsipStatus.INVESTIGATION_PENDING)
    whenever(csipRecordRepository.findByPrisonNumber("A1234BC")).thenReturn(listOf(csipRecord, csipRecord))

    val result = service.resolveInvestigations("A1234BC")

    assertThat(result.discoveredInvestigationIds).containsExactly(investigationId)
    assertThat(result.eligibleInvestigationIds).containsExactly(investigationId)
    verify(csipRecordRepository, times(0)).findById(any<UUID>())
  }

  @Test
  fun `mixed statuses produce correct eligible investigation list`() {
    val eligibleId = UUID.randomUUID()
    val ignoredStatusId = UUID.randomUUID()
    val eligibleRecord = csipRecordWithStatus(eligibleId, CsipStatus.INVESTIGATION_PENDING)
    val ignoredStatusRecord = csipRecordWithStatus(ignoredStatusId, CsipStatus.CSIP_OPEN)
    whenever(csipRecordRepository.findByPrisonNumber("A1234BC")).thenReturn(listOf(eligibleRecord, ignoredStatusRecord))

    val result = service.resolveInvestigations("A1234BC")

    assertThat(result.discoveredInvestigationIds).containsExactly(eligibleId, ignoredStatusId)
    assertThat(result.eligibleInvestigationIds).containsExactly(eligibleId)
    assertThat(result.ignoredInvestigations)
      .containsEntry(ignoredStatusId, "status is not INVESTIGATION_PENDING")
  }

  private fun csipRecordWithStatus(investigationId: UUID, status: CsipStatus): CsipRecord {
    val statusReferenceData = ReferenceData(
      key = ReferenceDataKey(ReferenceDataType.STATUS, status.name),
      description = status.name,
      listSequence = 1,
      deactivatedAt = null,
      id = 1,
    )
    val record = mock<CsipRecord>()
    whenever(record.id).thenReturn(investigationId)
    whenever(record.status).thenReturn(statusReferenceData)
    return record
  }
}
