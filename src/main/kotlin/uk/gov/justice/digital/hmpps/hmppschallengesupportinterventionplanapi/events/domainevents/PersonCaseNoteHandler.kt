package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNoteAnnotationsService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNotesService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.JdaService

@Service
class PersonCaseNoteHandler(
  private val personCaseNoteInvestigationService: PersonCaseNoteInvestigationService,
  private val jdaService: JdaService,
  private val caseNoteAnnotationsService: CaseNoteAnnotationsService,
  private val caseNoteAnalysedRepository: CaseNoteAnalysedRepository,
  private val caseNotesService: CaseNotesService,
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
    if (resolution.eligibleInvestigationIds.isEmpty()) {
      log.info(
        "Skipping case note analysis for caseNoteId={}, prisonerNumber={}, eventType={} because no eligible investigations were found",
        caseNoteId,
        prisonerNumber,
        event.eventType,
      )
    }

    for (investigationId in resolution.eligibleInvestigationIds) {
      if (!caseNoteAnalysedRepository.existsByInvestigationId(investigationId)) {
        log.warn(
          "{} domain event: investigationId={} does not exist in case_notes_analysed; triggering initial JDA analysis for prisonerNumber={}",
          event.eventType,
          investigationId,
          prisonerNumber,
        )

        val caseNote = caseNotesService.getCaseNote(prisonerNumber, caseNoteId)
        jdaService.submitCaseNotesForAnalysis(
          offenderIdentifier = prisonerNumber,
          prisonCode = caseNote.locationId,
          correlationId = investigationId.toString(),
        )
        continue
      }

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
        log.info(
          "Suggested case notes updated for investigationId={}, requestId={}, caseNoteId={}",
          investigationId,
          response.requestId,
          caseNoteId,
        )
      }
    }
  }
}
