package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.ZonedDateTime
import java.util.UUID

interface DomainEvent {
  val eventType: String
  val version: Int
  val detailUrl: String?
  val occurredAt: ZonedDateTime
  val description: String
  val additionalInformation: AdditionalInformation
  val personReference: PersonReference?
}

data class PersonReference(val identifiers: List<Identifier> = listOf()) {
  operator fun get(key: String) = identifiers.find { it.type == key }?.value
  fun findNomsNumber() = get(NOMS_NUMBER_TYPE)

  companion object {
    const val NOMS_NUMBER_TYPE = "NOMS"
    fun withPrisonNumber(prisonNumber: String) = PersonReference(listOf(Identifier(NOMS_NUMBER_TYPE, prisonNumber)))
  }

  data class Identifier(val type: String, val value: String)
}

interface AdditionalInformation

sealed interface CsipBaseInformation : AdditionalInformation {
  val source: Source
  val recordUuid: UUID
}

data class CsipDomainEvent(
  override val occurredAt: ZonedDateTime,
  override val eventType: String,
  override val detailUrl: String?,
  override val description: String,
  override val additionalInformation: CsipAdditionalInformation,
  override val personReference: PersonReference,
  override val version: Int = 1,
) : DomainEvent

data class CsipAdditionalInformation(
  override val recordUuid: UUID,
  val affectedComponents: Set<AffectedComponent>,
  override val source: Source,
) : CsipBaseInformation

data class CsipBasicInformation(
  val entityUuid: UUID,
  override val recordUuid: UUID,
  override val source: Source,
) : CsipBaseInformation

data class CsipBasicDomainEvent(
  override val occurredAt: ZonedDateTime,
  override val eventType: String,
  override val detailUrl: String?,
  override val description: String,
  override val additionalInformation: CsipBasicInformation,
  override val personReference: PersonReference,
  override val version: Int = 1,
) : DomainEvent
