package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import java.util.UUID

interface InterviewRepository : JpaRepository<Interview, UUID>

fun InterviewRepository.getInterview(id: UUID) =
  findByIdOrNull(id) ?: throw NotFoundException("Interview", id.toString())
