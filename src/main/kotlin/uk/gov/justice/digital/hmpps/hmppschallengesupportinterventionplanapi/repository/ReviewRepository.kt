package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import java.util.Optional
import java.util.UUID

interface ReviewRepository : JpaRepository<Review, UUID> {
  @EntityGraph(attributePaths = ["plan", "attendees"])
  override fun findById(uuid: UUID): Optional<Review>
}

fun ReviewRepository.getReview(reviewUuid: UUID): Review =
  findById(reviewUuid).orElseThrow { NotFoundException("Review", reviewUuid.toString()) }
