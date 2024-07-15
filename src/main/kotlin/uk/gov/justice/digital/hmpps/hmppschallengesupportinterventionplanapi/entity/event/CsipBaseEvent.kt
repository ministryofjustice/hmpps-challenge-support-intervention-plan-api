package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

sealed interface DomainEventable {
  val type: DomainEventType
  fun detailPath(): String
  fun toDomainEvent(baseUrl: String): DomainEvent
}

sealed interface CsipBaseEvent<T : AdditionalInformation> : DomainEventable {
  val recordUuid: UUID
  val description: String
  val occurredAt: LocalDateTime
  val source: Source
  override fun detailPath(): String = "/csip-records/$recordUuid"
}
