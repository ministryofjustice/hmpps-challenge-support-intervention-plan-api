package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteAnnotationSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteWithAnnotations
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNoteAmendment
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class CaseNoteAnnotationsService(
  private val caseNotesService: CaseNotesService,
  private val jdaService: JdaService,
  private val caseNoteAnnotationRepository: CaseNoteAnnotationRepository,
  private val personSummaryService: PersonSummaryService,
  private val csipRecordService: CsipRecordService,
  @Value("\${case-note-annotations.max-processing-duration:30s}")
  private val maxProcessingDuration: Duration,
) {

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val CREATED_DATE = "createdDate"
    const val LAST_AMENDED_DATE = "lastAmendedDate"
  }

  fun processQueuedCaseNoteAnnotations() {
    val response = jdaService.getCaseNoteAnnotationsFromQueue()
    log.info("Dequeued case note annotations for request ${response?.requestId} is $response")
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

  fun buildSuggestedCaseNotes(
    prisonerNumber: String,
    referralId: UUID,
    request: SuggestedCaseNotesRequest,
  ): SuggestedCaseNotesResponse {
    validatePrisonerExists(prisonerNumber)

    val sortOrder = request.sortOrder.trim().lowercase()
    val appliedSortOrder = if (sortOrder == "asc") "asc" else "desc"
    val sortField = normalizeSortField(request.sortField)

    val suggestedCaseNotes = getCaseNotesWithAnnotations(prisonerNumber, request.behaviourType, referralId)
      .sortedWith(caseNotesComparator(sortField, appliedSortOrder))
      .map { caseNoteWithAnnotations ->
        val highestConfidence = caseNoteWithAnnotations.annotations
          .mapNotNull { it.confidenceLevel }
          .maxByOrNull { it.ordinal }
          ?: ConfidenceLevel.LOW

        SuggestedCaseNote(
          relevance = highestConfidence.value,
          caseNoteId = caseNoteWithAnnotations.caseNote.caseNoteId,
          createdAt = caseNoteWithAnnotations.caseNote.creationDateTime,
          createdBy = caseNoteWithAnnotations.caseNote.authorName,
          annotatedCaseNote = composeCaseNoteAnnotation(caseNoteWithAnnotations, appliedSortOrder),
          amendments = composeAmendmentAnnotations(caseNoteWithAnnotations),
        )
      }

    return SuggestedCaseNotesResponse(
      prisonerNumber = prisonerNumber,
      behaviourType = request.behaviourType,
      sortField = sortField,
      sortOrder = appliedSortOrder,
      suggestedCaseNotes = suggestedCaseNotes,
    )
  }

  fun getCaseNotesWithAnnotations(
    prisonerNumber: String,
    behaviourType: BehaviourType,
    referralId: UUID,
  ): List<CaseNoteWithAnnotations> {
    // TODO case_notes_analysed: use referralId to scope Suggested Case Notes retrieval once referral-linked analysis results are available.
    val annotations = caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType(prisonerNumber, behaviourType)
    if (annotations.isEmpty()) return emptyList()

    return annotations
      .groupBy { it.caseNoteId }
      .map { (caseNoteId, caseNoteAnnotations) ->
        CaseNoteWithAnnotations(
          caseNote = caseNotesService.getCaseNote(prisonerNumber, caseNoteId),
          annotations = caseNoteAnnotations.map { it.toSummary() },
        )
      }
  }

  fun composeCaseNoteAnnotation(
    caseNoteWithAnnotations: CaseNoteWithAnnotations,
    sortOrder: String = "desc",
  ): String {
    val annotationTexts = caseNoteWithAnnotations.annotations
      .sortedWith(annotationComparator(sortOrder))
      .mapNotNull { it.annotatedText }
      .filter { it.isNotBlank() }

    return highlightAnnotationMatches(caseNoteWithAnnotations.caseNote.text, annotationTexts)
  }

  internal fun persistAnnotationsFromDequeue(initialResponse: JdaDequeueResponse?) {
    val start = Instant.now()
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

      if (Duration.between(start, Instant.now()) >= maxProcessingDuration) {
        log.info("Exited persistAnnotationsFromDequeue early after processing $count case note annotations")
        break
      }

      response = jdaService.getCaseNoteAnnotationsFromQueue()
    }

    if (count > 0) {
      log.info("Processed $count case note annotations")
    }
  }

  private fun validatePrisonerExists(prisonerNumber: String) {
    personSummaryService.validatePrisoner(prisonerNumber)
  }

  private fun composeAmendmentAnnotations(caseNoteWithAnnotations: CaseNoteWithAnnotations): List<SuggestedCaseNoteAmendment> {
    if (caseNoteWithAnnotations.caseNote.amendments.isEmpty()) return emptyList()

    val annotationTexts = caseNoteWithAnnotations.annotations
      .sortedWith(annotationComparator("desc"))
      .mapNotNull { it.annotatedText }
      .filter { it.isNotBlank() }

    return caseNoteWithAnnotations.caseNote.amendments
      .sortedByDescending { it.creationDateTime }
      .map { amendment ->
        SuggestedCaseNoteAmendment(
          createdAt = amendment.creationDateTime,
          annotatedText = highlightAnnotationMatches(amendment.additionalNoteText, annotationTexts),
        )
      }
  }

  private fun annotationComparator(sortOrder: String): Comparator<CaseNoteAnnotationSummary> = if (sortOrder == "asc") {
    compareBy { it.createdDate ?: LocalDateTime.MIN }
  } else {
    compareByDescending { it.createdDate ?: LocalDateTime.MIN }
  }

  private fun highlightAnnotationMatches(originalText: String, annotationTexts: List<String>): String {
    if (annotationTexts.isEmpty()) return originalText

    val matches = annotationTexts
      .mapNotNull { annotationText ->
        val startIndex = originalText.indexOf(annotationText)
        if (startIndex < 0) return@mapNotNull null
        TextMatch(start = startIndex, end = startIndex + annotationText.length, text = annotationText)
      }
      .sortedBy { it.start }

    if (matches.isEmpty()) return originalText

    val nonOverlappingMatches = mutableListOf<TextMatch>()
    var currentEnd = -1

    matches.forEach { match ->
      if (match.start >= currentEnd) {
        nonOverlappingMatches += match
        currentEnd = match.end
      }
    }

    if (nonOverlappingMatches.isEmpty()) return originalText

    val renderedText = StringBuilder()
    var cursor = 0

    nonOverlappingMatches.forEach { match ->
      renderedText.append(originalText.substring(cursor, match.start))
      renderedText.append("<span class=\"annotation-type\">")
      renderedText.append(match.text)
      renderedText.append("</span>")
      cursor = match.end
    }

    renderedText.append(originalText.substring(cursor))
    return renderedText.toString()
  }

  private fun caseNotesComparator(sortField: String, sortOrder: String): Comparator<CaseNoteWithAnnotations> = if (sortOrder == "asc") {
    compareBy { sortDateTime(it, sortField) }
  } else {
    compareByDescending { sortDateTime(it, sortField) }
  }

  private fun normalizeSortField(sortField: String): String = when (sortField.trim().lowercase()) {
    "lastamendeddate" -> LAST_AMENDED_DATE
    else -> CREATED_DATE
  }

  private fun sortDateTime(caseNoteWithAnnotations: CaseNoteWithAnnotations, sortField: String): LocalDateTime {
    val caseNote = caseNoteWithAnnotations.caseNote
    return when (sortField) {
      LAST_AMENDED_DATE -> latestTimelineDate(caseNote.creationDateTime, caseNote.amendments.map { it.creationDateTime })
      else -> caseNote.creationDateTime
    }
  }

  private fun latestTimelineDate(creationDateTime: LocalDateTime, amendmentDateTimes: List<LocalDateTime>): LocalDateTime = amendmentDateTimes
    .maxOrNull()
    ?.takeIf { it.isAfter(creationDateTime) }
    ?: creationDateTime

  private data class TextMatch(
    val start: Int,
    val end: Int,
    val text: String,
  )

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

  private fun CaseNoteAnnotation.toSummary() = CaseNoteAnnotationSummary(
    id = id,
    requestId = requestId,
    prisonerNumber = prisonerNumber,
    caseNoteId = caseNoteId,
    promptKey = promptKey,
    promptVersion = promptVersion,
    behaviourType = behaviourType,
    confidenceLevel = confidenceLevel,
    annotatedText = annotatedText,
    createdDate = createdDate,
  )
}
