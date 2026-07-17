package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import com.fasterxml.jackson.annotation.JsonProperty

data class CaseNoteAnalysisItem(
  @JsonProperty("item_id")
  val itemId: String,

  @JsonProperty("case_note_text")
  val caseNoteText: String,
)
