package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class JdaDequeueResponse(
  val requestId: UUID,
  val correlationId: String,
  val prompt: JdaPrompt,
  val status: JdaDequeueResponseStatus,
  val responseData: Any,
  val metadata: JdaDequeueResponseMetadata,
)

enum class JdaDequeueResponseStatus {

  @JsonProperty("succeeded")
  SUCCEEDED,

  @JsonProperty("failed")
  FAILED,

  @JsonProperty("rejected")
  REJECTED,
}

data class JdaDequeueResponseData(
  val todo: String,
)

data class JdaDequeueResponseMetadata(
  val requestType: JdaRequestType,
  val completedAt: Instant,
  val completionMs: Long,
)
