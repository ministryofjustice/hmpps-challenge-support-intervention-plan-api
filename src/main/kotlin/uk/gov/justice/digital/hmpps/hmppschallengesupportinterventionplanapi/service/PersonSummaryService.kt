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
  fun validatePrisoner(prisonNumber: String) {
    getPrisoner(prisonNumber)
  }

  fun updateExistingDetails(prisonNumber: String) {
    personSummaryRepository.findByIdOrNull(prisonNumber)?.also {
      val prisoner = getPrisoner(prisonNumber)
      it.update(
        prisoner.firstName,
        prisoner.lastName,
        prisoner.status,
        prisoner.restrictedPatient,
        prisoner.prisonId,
        prisoner.cellLocation,
        prisoner.supportingPrisonId,
      )
    }
  }

  fun savePersonSummary(personSummary: PersonSummary): PersonSummary = personSummaryRepository.findByIdOrNull(personSummary.prisonNumber) ?: personSummaryRepository.save(personSummary)

  fun getPersonSummaryByPrisonNumber(prisonNumber: String): PersonSummary {
    val person = personSummaryRepository.findByIdOrNull(prisonNumber)
    return if (person == null) {
      val prisoner = getPrisoner(prisonNumber)
      personSummaryRepository.save(
        PersonSummary(
          prisoner.prisonerNumber,
          prisoner.firstName,
          prisoner.lastName,
          prisoner.status,
          prisoner.restrictedPatient,
          prisoner.prisonId,
          prisoner.cellLocation,
          prisoner.supportingPrisonId,
        ),
      )
    } else {
      person
    }
  }

  fun removePersonSummaryByPrisonNumber(prisonNumber: String) = personSummaryRepository.findByIdOrNull(prisonNumber)?.also(personSummaryRepository::delete)

  private fun getPrisoner(prisonNumber: String) = requireNotNull(prisonerSearch.getPrisoner(prisonNumber)) { "Prisoner number invalid" }
}
