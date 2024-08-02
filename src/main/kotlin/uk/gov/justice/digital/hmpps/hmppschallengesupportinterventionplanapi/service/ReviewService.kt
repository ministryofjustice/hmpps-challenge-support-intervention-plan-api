package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingPlanException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import java.util.UUID

@Transactional
@Service
class ReviewService(private val csipRecordRepository: CsipRecordRepository) {
  fun addReview(recordUuid: UUID, request: CreateReviewRequest): Review {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = verifyExists(record.plan) { MissingPlanException(recordUuid) }
    val review = plan.addReview(csipRequestContext(), request)
    csipRecordRepository.save(record)
    return review.toModel()
  }
}

fun uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review.toModel() = Review(
  reviewUuid,
  reviewSequence,
  reviewDate,
  recordedBy,
  recordedByDisplayName,
  nextReviewDate,
  csipClosedDate,
  summary,
  actions,
  createdAt,
  createdBy,
  createdByDisplayName,
  lastModifiedAt,
  lastModifiedBy,
  lastModifiedByDisplayName,
  listOf(),
)
