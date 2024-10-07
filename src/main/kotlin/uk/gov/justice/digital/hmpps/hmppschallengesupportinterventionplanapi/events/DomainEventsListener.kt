package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PrisonerUpdatedInformation.Companion.CATEGORIES_OF_INTEREST
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.PersonLocationService

@Service
class DomainEventsListener(
  private val objectMapper: ObjectMapper,
  private val personLocationService: PersonLocationService,
) {
  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    val prisonerChanged = objectMapper.readValue<HmppsDomainEvent<PrisonerUpdatedInformation>>(notification.message)
    val matchingChanges = prisonerChanged.additionalInformation.categoriesChanged intersect CATEGORIES_OF_INTEREST
    if (matchingChanges.isNotEmpty()) {
      prisonerChanged.personReference.findNomsNumber()?.also(personLocationService::updateExistingDetails)
    }
  }

  companion object {
    const val PRISONER_UPDATED = "prisoner-offender-search.prisoner.updated"
  }
}
