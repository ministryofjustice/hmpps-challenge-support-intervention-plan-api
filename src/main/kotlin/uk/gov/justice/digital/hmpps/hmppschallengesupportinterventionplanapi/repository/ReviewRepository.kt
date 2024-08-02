package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import java.util.UUID

interface ReviewRepository : JpaRepository<Review, UUID> {
  fun findByReviewUuid(uuid: UUID): Review?
}

fun ReviewRepository.getReview(reviewUuid: UUID) =
  findByReviewUuid(reviewUuid) ?: throw NotFoundException("Review", reviewUuid.toString())
