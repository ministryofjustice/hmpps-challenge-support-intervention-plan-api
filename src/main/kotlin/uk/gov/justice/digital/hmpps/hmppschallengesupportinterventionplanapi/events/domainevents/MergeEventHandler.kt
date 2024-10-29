package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.matchesPrisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_MOVED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.CsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.PersonSummaryService
import java.time.LocalDateTime.now

@Transactional
@Service
class MergeEventHandler(
  private val personSummaryService: PersonSummaryService,
  private val csipRepository: CsipRecordRepository,
  private val eventPublisher: ApplicationEventPublisher,
) {
  fun handle(event: HmppsDomainEvent<MergeInformation>) {
    val personSummary = personSummaryService.getPersonSummaryByPrisonNumber(event.additionalInformation.nomsNumber)
    csipRepository.findAll(matchesPrisonNumber(event.additionalInformation.removedNomsNumber)).map {
      it.moveTo(personSummary)
    }.forEach {
      eventPublisher.publishEvent(
        CsipEvent(
          CSIP_MOVED,
          it.personSummary.prisonNumber,
          it.id,
          now(),
          event.additionalInformation.removedNomsNumber,
        ),
      )
    }
    personSummaryService.removePersonSummaryByPrisonNumber(event.additionalInformation.removedNomsNumber)
  }
}
