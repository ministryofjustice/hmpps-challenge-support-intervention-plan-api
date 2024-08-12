package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingPlanException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getReview
import java.util.UUID

@Transactional
@Service
class ReviewService(
  private val csipRecordRepository: CsipRecordRepository,
  private val reviewRepository: ReviewRepository,
) {
  fun addReview(recordUuid: UUID, request: CreateReviewRequest): Review {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = verifyExists(record.plan) { MissingPlanException(recordUuid) }
    val review = plan.addReview(request)
    csipRecordRepository.save(record)
    return review.toModel()
  }

  fun addAttendee(reviewUuid: UUID, request: CreateAttendeeRequest): Attendee {
    val review = reviewRepository.getReview(reviewUuid)
    val attendee = review.addAttendee(request)
    csipRecordRepository.save(review.plan.csipRecord)
    return attendee.toModel()
  }
}

fun uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review.toModel() = Review(
  uuid,
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
  attendees().map { it.toModel() },
)

fun uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Attendee.toModel() = Attendee(
  uuid,
  name,
  role,
  attended,
  contribution,
  createdAt,
  createdBy,
  createdByDisplayName,
  lastModifiedAt,
  lastModifiedBy,
  lastModifiedByDisplayName,
)
