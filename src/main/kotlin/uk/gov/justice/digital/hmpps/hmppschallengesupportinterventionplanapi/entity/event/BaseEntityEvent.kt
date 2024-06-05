package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime

abstract class BaseEntityEvent<T : DomainEvent<AdditionalInformation>> {
  abstract val description: String
  abstract val occurredAt: LocalDateTime
  abstract val source: Source
  abstract val reason: Reason

  abstract fun toDomainEvent(baseUrl: String): T
}
