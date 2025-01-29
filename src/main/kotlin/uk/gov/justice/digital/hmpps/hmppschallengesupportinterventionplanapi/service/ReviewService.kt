package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.AttendeeRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.ReviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.getAttendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.getReview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PublishCsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingPlanException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreateReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.UpdateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.UpdateReviewRequest
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
  fun updateReview(reviewUuid: UUID, request: UpdateReviewRequest): Review = reviewRepository.getReview(reviewUuid).update(request).toModel()

  @PublishCsipEvent(CSIP_UPDATED)
  fun addAttendee(reviewUuid: UUID, request: CreateAttendeeRequest): Attendee = reviewRepository.getReview(reviewUuid).addAttendee(request).toModel()

  @PublishCsipEvent(CSIP_UPDATED)
  fun updateAttendee(attendeeUuid: UUID, request: UpdateAttendeeRequest): Attendee = attendeeRepository.getAttendee(attendeeUuid).update(request).toModel()
}
