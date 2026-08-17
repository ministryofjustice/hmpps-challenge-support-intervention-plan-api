package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda.JdaClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class CaseNoteAnnotationsService(
  private val jdaClient: JdaClient,
  private val caseNoteAnnotationRepository: CaseNoteAnnotationRepository,
  private val csipRecordService: CsipRecordService,
) {

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun processQueuedCaseNoteAnnotations() {
    val response = jdaClient.getCaseNoteAnnotationsFromQueue()
    persistAnnotations(response)
  }

  internal fun persistAnnotations(initialResponse: JdaDequeueResponse?) {
    var response = initialResponse
    var count = 0
    while (response != null) {
      try {
        val prisonerNumber = csipRecordService.retrieveCsipRecord(response.correlationId).prisonNumber
        val caseNoteAnnotations = response.toAnnotations(prisonerNumber)
        caseNoteAnnotations.forEach { caseNoteAnnotation ->
          caseNoteAnnotationRepository.save(caseNoteAnnotation)
        }
        count++
      } catch (e: Exception) {
        log.error("Failed to persist case note annotation for request ${response.requestId}", e)
        // TODO may need to save the failed annotation persistence but this is not in current scope
      }
      response = jdaClient.getCaseNoteAnnotationsFromQueue()
    }

    if (count > 0) {
      log.info("Processed $count case note annotations")
    }
  }

  private fun JdaDequeueResponse.toAnnotations(prisonerNumber: String): List<CaseNoteAnnotation> = responseData.orEmpty().flatMap { item ->
    item.justifyingSpans.map { span ->
      CaseNoteAnnotation(
        requestId = requestId,
        prisonerNumber = prisonerNumber,
        caseNoteId = item.caseNoteId,
        promptKey = prompt.key,
        promptVersion = prompt.version,
        behaviourType = span.justifies,
        confidenceLevel = item.confidenceLevel,
        annotatedText = span.text,
        createdDate = LocalDateTime.now(ZoneOffset.UTC),
      )
    }
  }
}
