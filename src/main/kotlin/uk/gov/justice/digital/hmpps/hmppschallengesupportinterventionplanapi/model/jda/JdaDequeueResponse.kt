package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.JdaDequeueResponseStatus
import java.time.OffsetDateTime
import java.util.UUID

data class JdaDequeueResponse(
  val requestId: UUID,
  val correlationId: UUID,
  val prompt: JdaPrompt,
  val status: JdaDequeueResponseStatus,
  val responseData: List<JdaDequeueResponseData>?,
  val metadata: JdaDequeueResponseMetadata,
)

data class JdaDequeueResponseData(
  val caseNoteId: UUID,
  val confidenceLevel: ConfidenceLevel?,
  val justifyingSpans: List<JustifyingSpan>,
)

data class JdaDequeueResponseMetadata(
  val requestType: JdaRequestType,
  val completedAt: OffsetDateTime,
  val completionMs: Long,
)

data class JustifyingSpan(
  val text: String,
  val justifies: BehaviourType,
)
