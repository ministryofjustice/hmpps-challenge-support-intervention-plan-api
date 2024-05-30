package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import java.time.LocalDateTime

class ReferenceDataTranslationTest {
  @Test
  fun `should convert reference data entity to model`() {
    val entity = ReferenceData(
      referenceDataId = 1,
      domain = ReferenceDataType.OUTCOME_TYPE,
      code = "O",
      description = "Outcome description",
      listSequence = 99,
      createdAt = LocalDateTime.of(2021, 1, 1, 1, 1, 0),
      createdBy = "admin.user",
    )

    assertThat(entity.toReferenceDataModel()).isEqualTo(
      uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData(
        code = "O",
        description = "Outcome description",
        listSequence = 99,
        createdAt = LocalDateTime.of(2021, 1, 1, 1, 1, 0),
        createdBy = "admin.user",
        modifiedAt = null,
        modifiedBy = null,
        deactivatedAt = null,
        deactivatedBy = null,
      ),
    )
  }

  @Test
  fun `should convert reference data entity collection to model collection`() {
    val entities = listOf(
      ReferenceData(
        referenceDataId = 1,
        domain = ReferenceDataType.OUTCOME_TYPE,
        code = "O",
        description = "Outcome description",
        listSequence = 99,
        createdAt = LocalDateTime.of(2021, 1, 1, 1, 1, 0),
        createdBy = "admin.user",
      ),
      ReferenceData(
        referenceDataId = 2,
        domain = ReferenceDataType.OUTCOME_TYPE,
        code = "P",
        description = "Another outcome description",
        listSequence = 99,
        createdAt = LocalDateTime.of(2021, 1, 1, 1, 1, 0),
        createdBy = "admin.user",
      ).apply {
        deactivatedAt = LocalDateTime.now().minusDays(3)
      },
    )

    assertThat(entities.toReferenceDataModels(includeInactive = false)).containsExactlyInAnyOrder(
      uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData(
        code = "O",
        description = "Outcome description",
        listSequence = 99,
        createdAt = LocalDateTime.of(2021, 1, 1, 1, 1, 0),
        createdBy = "admin.user",
        modifiedAt = null,
        modifiedBy = null,
        deactivatedAt = null,
        deactivatedBy = null,
      ),
    )
  }
}
