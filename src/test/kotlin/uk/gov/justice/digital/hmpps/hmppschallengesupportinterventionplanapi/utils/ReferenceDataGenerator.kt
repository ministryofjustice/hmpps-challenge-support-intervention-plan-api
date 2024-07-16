package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import java.time.LocalDateTime

object ReferenceDataGenerator {
  fun generateReferenceData(
    domain: ReferenceDataType,
    code: String,
    description: String = "Description of $code",
    listSequence: Int = 1,
    createdAt: LocalDateTime = LocalDateTime.now().minusMonths(6),
    createdBy: String = "A1234",
    id: Long = IdGenerator.newId(),
  ): ReferenceData = ReferenceData(id, domain, code, description, listSequence, createdAt, createdBy)
}
