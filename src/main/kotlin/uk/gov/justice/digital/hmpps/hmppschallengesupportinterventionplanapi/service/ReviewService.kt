package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PublishCsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingPlanException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.AttendeeRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getAttendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getReview
import java.util.UUID

@Transactional
@Service
class ReviewService(
  private val csipRecordRepository: CsipRecordRepository,
  private val reviewRepository: ReviewRepository,
  private val attendeeRepository: AttendeeRepository,
) {
  @PublishCsipEvent(CSIP_UPDATED)
  fun addReview(recordUuid: UUID, request: CreateReviewRequest): Review {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    val plan = verifyExists(record.plan) { MissingPlanException(recordUuid) }
    return plan.addReview(request).toModel()
  }

  @PublishCsipEvent(CSIP_UPDATED)
  fun updateReview(reviewUuid: UUID, request: UpdateReviewRequest): Review =
    reviewRepository.getReview(reviewUuid).update(request).toModel()

  @PublishCsipEvent(CSIP_UPDATED)
  fun addAttendee(reviewUuid: UUID, request: CreateAttendeeRequest): Attendee =
    reviewRepository.getReview(reviewUuid).addAttendee(request).toModel()

  @PublishCsipEvent(CSIP_UPDATED)
  fun updateAttendee(attendeeUuid: UUID, request: UpdateAttendeeRequest): Attendee =
    attendeeRepository.getAttendee(attendeeUuid).update(request).toModel()
}

fun uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review.toModel() = Review(
  id,
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
  id,
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
