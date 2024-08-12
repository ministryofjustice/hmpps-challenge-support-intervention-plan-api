package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import java.util.UUID

interface ReviewRepository : JpaRepository<Review, UUID> {
  @EntityGraph(attributePaths = ["attendees"])
  fun findByUuid(uuid: UUID): Review?
}

fun ReviewRepository.getReview(reviewUuid: UUID) =
  findByUuid(reviewUuid) ?: throw NotFoundException("Review", reviewUuid.toString())
