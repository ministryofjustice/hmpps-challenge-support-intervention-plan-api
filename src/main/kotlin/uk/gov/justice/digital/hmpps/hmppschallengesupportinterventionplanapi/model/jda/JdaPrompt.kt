package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

data class JdaPrompt(
  val key: String,
  val version: Int,
) {
  companion object {
    fun caseNoteAnalysis() = JdaPrompt(
      key = "case-note-analysis",
      version = 3,
    )
  }
}
