package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PersonCaseNoteHandler {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handle(event: HmppsDomainEvent<PersonCaseNoteInformation>) {
    log.debug("Received {} event for case note {}", event.eventType, event.additionalInformation.id)
  }
}
