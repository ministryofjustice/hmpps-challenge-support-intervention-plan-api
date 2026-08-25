package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda.JdaClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestResponse
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

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
    persistAnnotationsFromDequeue(response)
  }

  fun persistSynchronousAnnotations(
    response: JdaRequestResponse,
    prisonerNumber: String,
  ) {
    try {
      val caseNoteAnnotations = responseDataToAnnotations(
        responseData = response.responseData.orEmpty(),
        requestId = response.requestId,
        prompt = response.prompt,
        prisonerNumber = prisonerNumber,
      )
      caseNoteAnnotations.forEach { caseNoteAnnotation ->
        try {
          caseNoteAnnotationRepository.save(caseNoteAnnotation)
        } catch (e: Exception) {
          log.error(
            "Failed to persist case note annotation for case note ${caseNoteAnnotation.caseNoteId}",
            e,
          )
        }
      }
      log.debug("Persisted ${caseNoteAnnotations.size} case note annotations from synchronous JDA response")
    } catch (e: Exception) {
      log.error("Failed to persist case note annotations from synchronous JDA response ${response.requestId}", e)
      throw e
    }
  }

  internal fun persistAnnotationsFromDequeue(initialResponse: JdaDequeueResponse?) {
    var response = initialResponse
    var count = 0
    while (response != null) {
      try {
        val prisonerNumber = csipRecordService.retrieveCsipRecord(response.correlationId).prisonNumber
        val caseNoteAnnotations = responseDataToAnnotations(
          responseData = response.responseData.orEmpty(),
          requestId = response.requestId,
          prompt = response.prompt,
          prisonerNumber = prisonerNumber,
        )
        caseNoteAnnotations.forEach { caseNoteAnnotation ->
          try {
            caseNoteAnnotationRepository.save(caseNoteAnnotation)
          } catch (e: Exception) {
            log.error(
              "Failed to persist case note annotation for case note ${caseNoteAnnotation.caseNoteId}",
              e,
            )
          }
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

  private fun responseDataToAnnotations(
    responseData: List<JdaDequeueResponseData>,
    requestId: UUID,
    prompt: JdaPrompt,
    prisonerNumber: String,
  ): List<CaseNoteAnnotation> = responseData.flatMap { item ->
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
