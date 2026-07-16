package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

data class JdaRequest(
  val correlationId: String,
  val prompt: JdaPrompt,
  val requestData: JdaRequestData,
)