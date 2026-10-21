package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNoteAnnotationsService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.JdaService

@Service
class PersonCaseNoteHandler(
  private val personCaseNoteInvestigationService: PersonCaseNoteInvestigationService,
  private val jdaService: JdaService,
  private val caseNoteAnnotationsService: CaseNoteAnnotationsService,
) {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handle(event: HmppsDomainEvent<PersonCaseNoteInformation>) {
    val caseNoteId = event.additionalInformation.id
    val prisonerNumber = event.personReference?.findNomsNumber()

    log.info("Received {} event for case note {}", event.eventType, caseNoteId)

    if (prisonerNumber == null) {
      log.warn(
        "Ignoring {} event for case note {} because no NOMS prisoner number was provided in personReference",
        event.eventType,
        caseNoteId,
      )
      return
    }

    log.info("Resolved prisoner number {} for case note {}", prisonerNumber, caseNoteId)

    val resolution = personCaseNoteInvestigationService.resolveInvestigations(prisonerNumber)

    log.info(
      "Found {} distinct investigations for prisoner number {}: {}",
      resolution.discoveredInvestigationIds.size,
      prisonerNumber,
      resolution.discoveredInvestigationIds,
    )
    log.debug(
      "Evaluated investigation statuses for prisoner number {}: {}",
      prisonerNumber,
      resolution.evaluations.associate { it.investigationId to (it.status ?: it.reason) },
    )
    if (resolution.ignoredInvestigations.isNotEmpty()) {
      log.info(
        "Ignored investigations for prisoner number {}: {}",
        prisonerNumber,
        resolution.ignoredInvestigations,
      )
    }
    log.info(
      "Eligible investigations for future JDA processing for prisoner number {}: {}",
      prisonerNumber,
      resolution.eligibleInvestigationIds,
    )

    resolution.eligibleInvestigationIds.forEach { investigationId ->

      log.info(
        "Triggering JDA re-analysis for investigationId={}, caseNoteId={}, prisonerNumber={}",
        investigationId,
        caseNoteId,
        prisonerNumber,
      )

      val response = jdaService.submitCaseNotesForReAnalysis(
        offenderIdentifier = prisonerNumber,
        investigationId = investigationId,
        caseNoteId = caseNoteId,
      )

      if (response != null) {
        log.info(
          "Persisting synchronous JDA response {} for investigationId={}",
          response.requestId,
          investigationId,
        )

        caseNoteAnnotationsService.persistSynchronousAnnotations(response, prisonerNumber)
      }
    }
  }
}
