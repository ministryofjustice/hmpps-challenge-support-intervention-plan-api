package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import java.util.UUID

@Schema(description = "Suggested case notes response")
data class SuggestedCaseNotesResponse(
  val prisonerId: String,
  val referralId: String,
  val behaviourType: BehaviourType,
  val sortField: String,
  val sortOrder: String,
  val suggestedCaseNotes: List<SuggestedCaseNote>,
)

@Schema(description = "A single suggested case note")
data class SuggestedCaseNote(
  val relevance: String,
  @JsonProperty("case_note_id")
  val caseNoteId: UUID,
  @JsonProperty("annotated_case_note")
  val annotatedCaseNote: String,
)
