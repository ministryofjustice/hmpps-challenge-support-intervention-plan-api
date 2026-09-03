package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.JdaDequeueResponseStatus
import java.time.LocalDateTime
import java.util.UUID

data class JdaDequeueResponse(
  val requestId: UUID,
  val correlationId: UUID,
  val prompt: JdaPrompt,
  val status: JdaDequeueResponseStatus,
  val responseData: List<JdaDequeueResponseData>?,
  @JsonAlias("metaData")
  val metadata: JdaDequeueResponseMetadata,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JdaDequeueResponseData(
  @JsonProperty("item_id")
  @JsonAlias("case_note_id")
  val caseNoteId: UUID,
  @JsonProperty("confidence_level")
  val confidenceLevel: ConfidenceLevel?,
  @JsonProperty("justifying_spans")
  val justifyingSpans: List<JustifyingSpan>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JdaDequeueResponseMetadata(
  val requestType: JdaRequestType,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val completedAt: LocalDateTime,
  val completionMs: Long,
)

data class JustifyingSpan(
  val text: String,
  val justifies: BehaviourType,
)
