package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda.JdaClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse

@Service
class CaseNoteAnnotationsService(
  private val jdaClient: JdaClient,
) {

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getCaseNoteAnnotationsFromQueue() {
    log.info("Fetching case note annotations from queue")
    var response = jdaClient.getCaseNoteAnnotationsFromQueue()

    while (response != null) {
      log.debug("Fetched case note annotations for requestId=${response.requestId}")
      persistCaseNoteAnnotations(response)
      response = jdaClient.getCaseNoteAnnotationsFromQueue()
    }

    log.info("No more case note annotations available in queue")
  }

  private fun persistCaseNoteAnnotations(dequeuedResponse: JdaDequeueResponse) {
    // TODO
  }
}
