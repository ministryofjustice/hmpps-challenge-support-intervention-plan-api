package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummaryRepository

@Service
@Transactional
class PersonSummaryService(
  private val prisonerSearch: PrisonerSearchClient,
  private val personSummaryRepository: PersonSummaryRepository,
) {
  fun updateExistingDetails(prisonNumber: String) {
    personSummaryRepository.findByIdOrNull(prisonNumber)?.also {
      val prisoner = requireNotNull(prisonerSearch.getPrisoner(prisonNumber)) { "Prisoner number invalid" }
      it.update(prisoner.firstName, prisoner.lastName, prisoner.status, prisoner.prisonId, prisoner.cellLocation)
    }
  }

  fun savePersonSummary(personSummary: PersonSummary): PersonSummary =
    personSummaryRepository.findByIdOrNull(personSummary.prisonNumber) ?: personSummaryRepository.save(personSummary)

  fun getPersonSummaryByPrisonNumber(prisonNumber: String): PersonSummary {
    val person = personSummaryRepository.findByIdOrNull(prisonNumber)
    return if (person == null) {
      val prisoner = requireNotNull(prisonerSearch.getPrisoner(prisonNumber)) { "Prisoner number invalid" }
      personSummaryRepository.save(
        PersonSummary(
          prisoner.prisonerNumber,
          prisoner.firstName,
          prisoner.lastName,
          prisoner.status,
          prisoner.prisonId,
          prisoner.cellLocation,
        ),
      )
    } else {
      person
    }
  }

  fun removePersonSummaryByPrisonNumber(prisonNumber: String) =
    personSummaryRepository.findByIdOrNull(prisonNumber)?.also(personSummaryRepository::delete)
}
