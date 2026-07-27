package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import java.util.*

data class JdaRequestResponse(
  val requestId: UUID,
  val correlationId: String,
  val prompt: JdaPrompt,
  val status: JdaRequestStatus,
  val metadata: JdaRequestMetadata,
)
