package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.PersonLocation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.toPersonLocation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.DomainEventsListener.Companion.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PersonReference.Companion.withPrisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PrisonerUpdatedInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.verifyAgainst
import java.time.Duration.ofSeconds
import java.time.ZonedDateTime

class PersonLocationUpdateIntTest : IntegrationTestBase() {
  @Test
  fun `message ignored if category not of interest`() {
    val personLocation = personLocationRepository.save(prisoner().toPersonLocation())
    val updatedPrisoner = prisoner(
      prisonerNumber = personLocation.prisonNumber,
      firstName = "Update-First",
      lastName = "Update-Last",
      status = "INACTIVE OUT",
      prisonId = null,
      cellLocation = null,
    )
    prisonerSearch.stubGetPrisoner(updatedPrisoner)

    sendPersonChangedEvent(
      personChangedEvent(
        personLocation.prisonNumber,
        setOf(
          "IDENTIFIERS",
          "ALERTS",
          "SENTENCE",
          "RESTRICTED_PATIENT",
          "INCENTIVE_LEVEL",
          "PHYSICAL_DETAILS",
          "CONTACT_DETAILS",
        ),
      ),
    )

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    val saved = requireNotNull(personLocationRepository.findByIdOrNull(personLocation.prisonNumber))
    saved.verifyAgainstOther(personLocation)
  }

  @ParameterizedTest
  @ValueSource(strings = ["PERSONAL_DETAILS", "STATUS", "LOCATION"])
  fun `message with matching category causes person location details to be updated`(changeCategory: String) {
    val personLocation = personLocationRepository.save(prisoner().toPersonLocation())
    val updatedPrisoner = prisoner(
      prisonerNumber = personLocation.prisonNumber,
      firstName = "Update-First",
      lastName = "Update-Last",
      status = "INACTIVE OUT",
      prisonId = null,
      cellLocation = null,
    )
    prisonerSearch.stubGetPrisoner(updatedPrisoner)

    sendPersonChangedEvent(personChangedEvent(personLocation.prisonNumber, setOf(changeCategory)))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    val saved = requireNotNull(personLocationRepository.findByIdOrNull(personLocation.prisonNumber))
    saved.verifyAgainst(updatedPrisoner)
  }
}

private fun personChangedEvent(
  prisonNumber: String,
  changeCategories: Set<String>,
  occurredAt: ZonedDateTime = ZonedDateTime.now(),
  eventType: String = PRISONER_UPDATED,
  detailUrl: String? = null,
  description: String = "A prisoner was updated",
) = HmppsDomainEvent(
  occurredAt,
  eventType,
  detailUrl,
  description,
  PrisonerUpdatedInformation(changeCategories),
  withPrisonNumber(prisonNumber),
)

private fun PersonLocation.verifyAgainstOther(other: PersonLocation) {
  assertThat(prisonNumber).isEqualTo(other.prisonNumber)
  assertThat(firstName).isEqualTo(other.firstName)
  assertThat(lastName).isEqualTo(other.lastName)
  assertThat(status).isEqualTo(other.status)
  assertThat(prisonCode).isEqualTo(other.prisonCode)
  assertThat(cellLocation).isEqualTo(other.cellLocation)
}
