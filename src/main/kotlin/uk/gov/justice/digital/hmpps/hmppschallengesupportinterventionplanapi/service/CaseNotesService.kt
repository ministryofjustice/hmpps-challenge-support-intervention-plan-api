package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteAnnotationSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CaseNoteWithAnnotations
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNote
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

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

    return caseNotesClient.getCaseNotes(
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
  }

  fun validatePrisonerExists(prisonerNumber: String) {
    requireNotNull(prisonerSearch.getPrisoner(prisonerNumber)) { "Prisoner number invalid" }
  }

  // TODO: Replace static test data with JDA-generated suggested case notes when JDA retrieval is available.
  fun buildSuggestedCaseNotes(prisonerNumber: String, request: SuggestedCaseNotesRequest): SuggestedCaseNotesResponse = SuggestedCaseNotesResponse(
    prisonerNumber = prisonerNumber,
    referralId = request.referralId,
    behaviourType = request.behaviourType,
    sortField = request.sortField,
    sortOrder = request.sortOrder,
    suggestedCaseNotes = staticSuggestedCaseNotes(request.behaviourType),
  )

  private fun staticSuggestedCaseNotes(behaviourType: BehaviourType): List<SuggestedCaseNote> = when (behaviourType) {
    BehaviourType.RISKS_AND_TRIGGERS ->
      listOf(
        SuggestedCaseNote(
          relevance = "high",
          caseNoteId = UUID.fromString("a3f92dc1-8b14-4e2f-bc63-7291a047f183"),
          annotatedCaseNote = "New Induction  - Marcus arrived at HMP Feltham on 14/03/2025, " +
            "this is his third time in custody and <span data='1'> he appeared visibly anxious and withdrawn upon arrival.</span> " +
            "He stated he believed he should be on an enhanced CSRA level but has been placed on standard. " +
            "Marcus expressed a desire to transfer to a facility closer to his hometown so that his partner and children could visit more regularly. " +
            "He disclosed a previous history of self-harm but stated he has no current thoughts or intentions. " +
            "Marcus mentioned he has some historic links to a local group but was unable to identify any known conflicts within this establishment. " +
            "Marcus confirmed he understood the support options available to him and said he would approach staff if he needed assistance. " +
            "He was polite throughout the conversation and expressed gratitude at the end of the session.",
        ),
        SuggestedCaseNote(
          relevance = "high",
          caseNoteId = UUID.fromString("b7d14fe3-2c90-4f71-a891-cc84e6015d29"),
          annotatedCaseNote = "During this evening's welfare check, Marcus was in a very distressed state and <span data='1'> was unable to engage in any meaningful conversation regarding his wellbeing or future plans.</span>  He was unable to confirm whether he had any immediate intentions to harm himself, and when asked directly whether he could keep himself safe overnight, he gave no clear assurance. Staff discussed the possibility of increased monitoring with the on-call manager. Following a brief MDT consultation via telephone, it was agreed that Marcus should be placed on a two-person watch for the remainder of the night, with a formal review to take place at morning handover. Marcus was informed of this decision and did not object.",
        ),
        SuggestedCaseNote(
          relevance = "medium",
          caseNoteId = UUID.fromString("c1e87ab4-3d56-4a09-bc72-f204e9182c61"),
          annotatedCaseNote = "Marcus was relocated to the Separation and Care Unit earlier today following concerns raised overnight. <span data='1'> He declined all activities offered during the morning regime</span>  including exercise and association.",
        ),
        SuggestedCaseNote(
          relevance = "medium",
          caseNoteId = UUID.fromString("d5a23bc7-6f48-4d12-bf91-1038c7e3e20b"),
          annotatedCaseNote = "Prior to meeting with Mr. Okafor, I reviewed his recent case notes to prepare for our first key work session together. <span data='1'> I noted he has received two warnings in recent weeks</span>  - one related to an altercation with another resident and one relating to possession of unauthorised items. He is currently without employment and his ACCT was opened recently following a self-harm disclosure. I met with Mr. Okafor in his cell and introduced myself as his allocated keyworker and explained the purpose of the session. He initially said everything was fine and seemed reluctant to engage, however after I clarified what key work involves he agreed to participate. His cell was in a poor state of cleanliness. I asked him about the recent warnings and he explained that tensions had arisen with another resident over a misunderstanding, and that the second incident was due to taking items he felt were owed to him. I encouraged him to approach staff in future rather than taking matters into his own hands. ACTION PLAN: 1. Reapply for employment on the wing. 2. Improve cell cleanliness.",
        ),
        SuggestedCaseNote(
          relevance = "low",
          caseNoteId = UUID.fromString("e8c56da2-1f73-4b07-bc43-2190a7e4f512"),
          annotatedCaseNote = "On 22/05/2025 at approximately 11:30, Mr. Okafor approached me near the servery on B wing and requested to speak about <span data='1'> a personal property matter</span> . He stated that several items of clothing had gone missing following a cell search carried out the previous week. I explained that I was not present during that search but that I would look into the matter and follow up with him. Mr. Okafor became increasingly agitated and began raising his voice, accusing staff of stealing from him. He used threatening and offensive language toward me directly. I activated my body-worn camera and asked him calmly to return to his cell while the matter was investigated. Mr. Okafor became physically confrontational. A colleague responded to my request for assistance and together we guided Mr. Okafor back to his cell using appropriate restraint techniques. He was secured in his cell without further incident. Mr. Okafor will be placed on report for threatening behaviour and failure to comply with a lawful instruction.",
        ),
      )

    BehaviourType.USUAL_BEHAVIOUR_PRESENTATION ->
      listOf(
        SuggestedCaseNote(
          relevance = "high",
          caseNoteId = UUID.fromString("d6f17a53-2b16-5d48-be94-3a5c9f672145"),
          annotatedCaseNote = "Prisoner typically presents as cooperative during routine interactions and responds well to calm direction from staff.",
        ),
        SuggestedCaseNote(
          relevance = "medium",
          caseNoteId = UUID.fromString("e7028b64-3c27-6e59-cf05-4a6d0e783256"),
          annotatedCaseNote = "Observed to engage positively with education activities and maintained settled behaviour throughout the week.",
        ),
        SuggestedCaseNote(
          relevance = "low",
          caseNoteId = UUID.fromString("f8139c75-4d38-7f6a-d016-5b7e1a894367"),
          annotatedCaseNote = "Prisoner generally complies with cell searches and raised no concerns during last security check.",
        ),
      )

    BehaviourType.PROTECTIVE_FACTORS ->
      listOf(
        SuggestedCaseNote(
          relevance = "high",
          caseNoteId = UUID.fromString("a924ad86-5e49-4a7b-8127-6b8f2a905478"),
          annotatedCaseNote = "Prisoner maintains regular contact with family and reports this as a key source of motivation to manage behaviour.",
        ),
        SuggestedCaseNote(
          relevance = "medium",
          caseNoteId = UUID.fromString("b035be97-6f5a-4b8c-9238-7c9e3b016589"),
          annotatedCaseNote = "Active participation in offending behaviour programme noted; facilitator reports positive engagement.",
        ),
        SuggestedCaseNote(
          relevance = "low",
          caseNoteId = UUID.fromString("c146cf08-7a6b-4c9d-a349-8d0f4c127690"),
          annotatedCaseNote = "Prisoner has identified a trusted member of staff and has used this relationship to de-escalate on two occasions.",
        ),
      )
  }

  fun getCaseNotesWithAnnotations(prisonerNumber: String, behaviourType: BehaviourType): List<CaseNoteWithAnnotations> {
    val annotations = caseNoteAnnotationRepository.findByPrisonerNumberAndBehaviourType(prisonerNumber, behaviourType)
    if (annotations.isEmpty()) return emptyList()

    return annotations
      .filter { it.caseNoteId != null }
      .groupBy { it.caseNoteId!! }
      .map { (caseNoteId, caseNoteAnnotations) ->
        CaseNoteWithAnnotations(
          caseNote = caseNotesClient.getCaseNote(prisonerNumber, caseNoteId),
          annotations = caseNoteAnnotations.map { it.toSummary() },
        )
      }
  }

  fun renderAnnotatedCaseNote(caseNoteWithAnnotations: CaseNoteWithAnnotations): String {
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

  private data class TextMatch(
    val start: Int,
    val end: Int,
    val text: String,
  )

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
