package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.PersonLocationRepository

@Service
@Transactional
class PersonLocationService(
  private val prisonerSearch: PrisonerSearchClient,
  private val personLocationRepository: PersonLocationRepository,
) {
  fun updateExistingDetails(prisonNumber: String) {
    personLocationRepository.findByIdOrNull(prisonNumber)?.also {
      val prisoner = requireNotNull(prisonerSearch.getPrisoner(prisonNumber)) { "Prisoner number invalid" }
      it.update(prisoner.firstName, prisoner.lastName, prisoner.status, prisoner.prisonId, prisoner.cellLocation)
    }
  }
}
