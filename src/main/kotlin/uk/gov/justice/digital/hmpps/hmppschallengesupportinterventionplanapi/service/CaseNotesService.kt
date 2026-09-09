package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteAnnotationSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteWithAnnotations
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
import java.time.Clock
import java.time.LocalDateTime

@Service
class CaseNotesService(
  private val caseNotesClient: CaseNotesClient,
  private val prisonerSearch: PrisonerSearchClient,
  private val clock: Clock,
  private val caseNoteAnnotationRepository: CaseNoteAnnotationRepository,
) {
  fun getCaseNotes(
    request: CaseNotesLookupRequest,
    params: CaseNotesFilterParams = CaseNotesFilterParams(),
  ): CaseNotesResponse {
    val now = LocalDateTime.now(clock)

    val caseNotes = caseNotesClient.getCaseNotes(
      request.offenderIdentifier,
      CaseNotesRequest(
        includeSensitive = request.includeSensitive,
        typeSubTypes = emptyList(),
        occurredFrom = now.minusDays(params.period),
        occurredTo = now,
        page = 1,
        size = params.pageSize,
        sort = "occurredAt,desc",
      ),
    )
    return caseNotes.copy(content = caseNotes.content.filter { it.type != "ALERT" })
  }

  fun validatePrisonerExists(prisonerNumber: String) {
    requireNotNull(prisonerSearch.getPrisoner(prisonerNumber)) { "Prisoner number invalid" }
  }

  fun buildSuggestedCaseNotes(prisonerNumber: String, request: SuggestedCaseNotesRequest): SuggestedCaseNotesResponse {
    val sortOrder = request.sortOrder.trim().lowercase()
    val appliedSortOrder = if (sortOrder == "asc") "asc" else "desc"
    val sortField = normalizeSortField(request.sortField)

    val suggestedCaseNotes = getCaseNotesWithAnnotations(prisonerNumber, request.behaviourType)
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
          annotatedCaseNote = composeAnnotationCaseNote(caseNoteWithAnnotations),
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

  fun getCaseNotesWithAnnotations(prisonerNumber: String, behaviourType: BehaviourType): List<CaseNoteWithAnnotations> {
    val annotations = caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType(prisonerNumber, behaviourType)
    if (annotations.isEmpty()) return emptyList()

    return annotations
      .groupBy { it.caseNoteId }
      .map { (caseNoteId, caseNoteAnnotations) ->
        CaseNoteWithAnnotations(
          caseNote = caseNotesClient.getCaseNote(prisonerNumber, caseNoteId),
          annotations = caseNoteAnnotations.map { it.toSummary() },
        )
      }
  }

  fun composeAnnotationCaseNote(caseNoteWithAnnotations: CaseNoteWithAnnotations): String {
    val originalText = caseNoteWithAnnotations.caseNote.text
    if (caseNoteWithAnnotations.annotations.isEmpty()) return originalText

    val matches = caseNoteWithAnnotations.annotations
      .mapNotNull { it.annotatedText }
      .filter { it.isNotBlank() }
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

  private companion object {
    const val CREATED_DATE = "createdDate"
    const val LAST_AMENDED_DATE = "lastAmendedDate"
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
