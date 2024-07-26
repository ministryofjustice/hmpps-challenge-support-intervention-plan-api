package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import java.time.LocalDateTime

class ReferenceDataServiceTest {
  private val referenceDataRepository = mock<ReferenceDataRepository>()

  private val underTest = ReferenceDataService(referenceDataRepository)

  @Test
  fun `get active reference data by domain`() {
    whenever(referenceDataRepository.findByDomain(ReferenceDataType.OUTCOME_TYPE)).thenReturn(
      listOf(
        ReferenceData(
          domain = ReferenceDataType.OUTCOME_TYPE,
          code = "O",
          description = "Outcome description",
          listSequence = 99,
          createdAt = LocalDateTime.of(2021, 1, 1, 1, 1, 0),
          createdBy = "admin.user",
          id = 1,
        ),
        ReferenceData(
          domain = ReferenceDataType.OUTCOME_TYPE,
          code = "P",
          description = "Another outcome description",
          listSequence = 99,
          createdAt = LocalDateTime.of(2021, 1, 1, 1, 1, 0),
          createdBy = "admin.user",
          id = 2,
        ).apply {
          deactivatedAt = LocalDateTime.now().minusDays(3)
        },
      ),
    )

    val result = underTest.getReferenceDataForDomain(ReferenceDataType.OUTCOME_TYPE, false)

    assertThat(result).containsExactlyInAnyOrder(
      uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData(
        code = "O",
        description = "Outcome description",
        listSequence = 99,
        createdAt = LocalDateTime.of(2021, 1, 1, 1, 1, 0),
        createdBy = "admin.user",
        lastModifiedAt = null,
        lastModifiedBy = null,
        deactivatedAt = null,
        deactivatedBy = null,
      ),
    )
  }
}
