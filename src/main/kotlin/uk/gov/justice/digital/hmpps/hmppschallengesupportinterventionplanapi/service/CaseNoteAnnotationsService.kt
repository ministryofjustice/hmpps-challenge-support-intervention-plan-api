package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda.JdaClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse

@Service
class CaseNoteAnnotationsService(
  private val jdaClient: JdaClient,
  private val caseNoteAnnotationsPersistenceService: CaseNoteAnnotationsPersistenceService,
) {

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getCaseNoteAnnotationsFromQueue() {
    var response = jdaClient.getCaseNoteAnnotationsFromQueue()
    var count = 0
    while (response != null) {
      try {
        caseNoteAnnotationsPersistenceService.persistCaseNoteAnnotations(response)
        count++
      } catch (e: Exception) {
        log.error("Failed to persist case note annotation for request ${response.requestId}", e)
        // TODO need to save the failed annotation persistence somewhere so it isn't just lost - DLQ, or a staging table etc
      }
      response = jdaClient.getCaseNoteAnnotationsFromQueue()
    }

    if (count > 0) {
      log.info("Processed $count case note annotations")
    }
  }
}
