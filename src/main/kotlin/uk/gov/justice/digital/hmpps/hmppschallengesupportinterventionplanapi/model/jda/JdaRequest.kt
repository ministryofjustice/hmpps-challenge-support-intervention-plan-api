package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

data class JdaRequest<T>(
  val correlationId: String,
  val prompt: JdaPrompt,
  val requestData: T,
)
