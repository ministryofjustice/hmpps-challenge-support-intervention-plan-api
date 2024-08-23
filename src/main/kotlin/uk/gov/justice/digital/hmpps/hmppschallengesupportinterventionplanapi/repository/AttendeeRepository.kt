package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import java.util.UUID

interface AttendeeRepository : JpaRepository<Attendee, UUID>

fun AttendeeRepository.getAttendee(id: UUID) =
  findByIdOrNull(id) ?: throw NotFoundException("Attendee", id.toString())
