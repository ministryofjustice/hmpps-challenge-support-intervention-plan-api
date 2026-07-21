package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

data class JdaError(
  val code: String,
  val message: String,
  val stage: String?,
)
