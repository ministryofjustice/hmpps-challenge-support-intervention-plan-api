package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import java.time.ZonedDateTime

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

data class HmppsDomainEvent<T : AdditionalInformation>(
  override val occurredAt: ZonedDateTime,
  override val eventType: String,
  override val detailUrl: String?,
  override val description: String,
  override val additionalInformation: T,
  override val personReference: PersonReference?,
  override val version: Int = 1,
) : DomainEvent

interface AdditionalInformation

data class PrisonerUpdatedInformation(val nomsNumber: String, val categoriesChanged: Set<String>) : AdditionalInformation {
  companion object {
    val CATEGORIES_OF_INTEREST = setOf("PERSONAL_DETAILS", "STATUS", "LOCATION")
  }
}

data class MergeInformation(
  val nomsNumber: String,
  val removedNomsNumber: String,
) : AdditionalInformation

data class BookingMovedInformation(
  val movedFromNomsNumber: String,
  val movedToNomsNumber: String,
  val bookingStartDateTime: ZonedDateTime,
) : AdditionalInformation

data class PersonReconciliationInformation(val prisonNumbers: Set<String>) : AdditionalInformation
