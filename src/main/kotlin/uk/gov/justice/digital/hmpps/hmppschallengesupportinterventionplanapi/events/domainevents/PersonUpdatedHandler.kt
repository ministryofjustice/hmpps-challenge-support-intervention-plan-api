package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.PrisonerUpdatedInformation.Companion.CATEGORIES_OF_INTEREST
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.PersonSummaryService

@Transactional
@Service
class PersonUpdatedHandler(private val personSummaryService: PersonSummaryService) {
  fun handle(prisonerChanged: HmppsDomainEvent<PrisonerUpdatedInformation>) {
    val matchingChanges = prisonerChanged.additionalInformation.categoriesChanged intersect CATEGORIES_OF_INTEREST
    if (matchingChanges.isNotEmpty()) {
      personSummaryService.updateExistingDetails(prisonerChanged.additionalInformation.nomsNumber)
    }
  }
}
