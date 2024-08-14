package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import java.util.UUID

interface ContributoryFactorRepository : JpaRepository<ContributoryFactor, UUID>

fun ContributoryFactorRepository.getContributoryFactor(id: UUID) =
  findByIdOrNull(id) ?: throw NotFoundException("Contributory Factor", id.toString())
