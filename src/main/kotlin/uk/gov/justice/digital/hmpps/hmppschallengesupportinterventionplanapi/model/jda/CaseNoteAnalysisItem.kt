package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import com.fasterxml.jackson.annotation.JsonProperty

data class CaseNoteAnalysisItem(
  @JsonProperty("case_note_id")
  val caseNoteId: String,

  @JsonProperty("case_note_text")
  val caseNoteText: String,
)
