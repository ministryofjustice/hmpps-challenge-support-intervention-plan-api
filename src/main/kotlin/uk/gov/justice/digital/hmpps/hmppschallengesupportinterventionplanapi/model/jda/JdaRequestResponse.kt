package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import java.util.*

data class JdaRequestResponse(
  val requestId: UUID,
  val correlationId: UUID,
  val prompt: JdaPrompt,
  val status: JdaRequestStatus,
  val responseData: List<JdaDequeueResponseData>?,
  val metadata: JdaMetadata,
)
