package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

sealed interface DomainEventable<T : AdditionalInformation> {
  val type: DomainEventType
  fun detailPath(): String
  fun toDomainEvent(baseUrl: String): DomainEvent<T>
}

interface CsipBaseEvent<T : AdditionalInformation> : DomainEventable<T> {
  val recordUuid: UUID
  val description: String
  val occurredAt: LocalDateTime
  val source: Source
  override fun detailPath(): String = "/csip-records/$recordUuid"
}
