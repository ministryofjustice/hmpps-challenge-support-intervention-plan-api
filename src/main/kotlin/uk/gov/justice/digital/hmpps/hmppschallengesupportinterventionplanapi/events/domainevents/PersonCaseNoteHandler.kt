package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PersonCaseNoteHandler {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handleCreated(event: HmppsDomainEvent<CaseNoteAdditionalInformation>) {
    log.info("Received person.case-note.created event: {}", event)
  }

  fun handleUpdated(event: HmppsDomainEvent<CaseNoteAdditionalInformation>) {
    log.info("Received person.case-note.updated event: {}", event)
  }
}
