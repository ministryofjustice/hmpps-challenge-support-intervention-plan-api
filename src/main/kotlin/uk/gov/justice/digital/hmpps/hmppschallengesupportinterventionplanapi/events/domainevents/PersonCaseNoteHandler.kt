package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PersonCaseNoteHandler(
  private val personCaseNoteInvestigationService: PersonCaseNoteInvestigationService,
) {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handle(event: HmppsDomainEvent<PersonCaseNoteInformation>) {
    val caseNoteId = event.additionalInformation.id
    val prisonNumber = event.personReference?.findNomsNumber()

    log.info("Received {} event for case note {}", event.eventType, caseNoteId)

    if (prisonNumber == null) {
      log.warn(
        "Ignoring {} event for case note {} because no NOMS prison number was provided in personReference",
        event.eventType,
        caseNoteId,
      )
      return
    }

    log.info("Resolved prison number {} for case note {}", prisonNumber, caseNoteId)

    val resolution = personCaseNoteInvestigationService.resolveInvestigations(prisonNumber)

    log.info(
      "Found {} distinct investigations for prison number {}: {}",
      resolution.discoveredInvestigationIds.size,
      prisonNumber,
      resolution.discoveredInvestigationIds,
    )
    log.info(
      "Evaluated investigation statuses for prison number {}: {}",
      prisonNumber,
      resolution.evaluations.associate { it.investigationId to (it.status ?: it.reason) },
    )
    if (resolution.ignoredInvestigations.isNotEmpty()) {
      log.info(
        "Ignored investigations for prison number {}: {}",
        prisonNumber,
        resolution.ignoredInvestigations,
      )
    }
    log.info(
      "Eligible investigations for future JDA processing for prison number {}: {}",
      prisonNumber,
      resolution.eligibleInvestigationIds,
    )
  }
}
