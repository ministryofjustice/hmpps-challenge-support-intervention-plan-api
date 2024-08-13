package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import java.util.UUID

interface IdentifiedNeedRepository : JpaRepository<IdentifiedNeed, UUID>

fun IdentifiedNeedRepository.getIdentifiedNeed(id: UUID) =
  findByIdOrNull(id) ?: throw NotFoundException("Identified Need", id.toString())
