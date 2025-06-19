package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.matchesPrisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_MOVED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.PersonSummaryService
import java.time.LocalDateTime.now

@Transactional
@Service
class MoveEventHandler(
  private val personSummaryService: PersonSummaryService,
  private val csipRepository: CsipRecordRepository,
  private val eventPublisher: ApplicationEventPublisher,
) {
  fun handleMerge(event: HmppsDomainEvent<MergeInformation>) {
    val mi = event.additionalInformation
    csipRepository.findAll(matchesPrisonNumber(mi.removedNomsNumber))
      .moveTo(mi.nomsNumber, mi.removedNomsNumber)
  }

  fun handleBookingMoved(event: HmppsDomainEvent<BookingMovedInformation>) {
    val bmi = event.additionalInformation
    csipRepository.findByPrisonNumberAndReferralReferralDateBetween(
      bmi.movedFromNomsNumber,
      bmi.bookingStartDateTime,
      event.occurredAt,
    ).moveTo(bmi.movedToNomsNumber, bmi.movedFromNomsNumber)
  }

  private fun List<CsipRecord>.moveTo(prisonNumber: String, fromNomsNumber: String) {
    if (isNotEmpty()) {
      val personSummary = personSummaryService.getPersonSummaryByPrisonNumber(prisonNumber)
      map {
        it.moveTo(personSummary)
      }.forEach {
        eventPublisher.publishEvent(
          CsipEvent(
            CSIP_MOVED,
            it.personSummary.prisonNumber,
            it.id,
            now(),
            fromNomsNumber,
          ),
        )
      }
    }
    if (csipRepository.countByPrisonNumber(fromNomsNumber) == 0) {
      personSummaryService.removePersonSummaryByPrisonNumber(fromNomsNumber)
    }
  }
}
