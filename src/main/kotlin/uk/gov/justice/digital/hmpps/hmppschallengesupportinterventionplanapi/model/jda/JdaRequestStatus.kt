package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import com.fasterxml.jackson.annotation.JsonProperty

enum class JdaRequestStatus {
  @JsonProperty("queued")
  QUEUED,

  @JsonProperty("processing")
  PROCESSING,

  @JsonProperty("succeeded")
  SUCCEEDED,

  @JsonProperty("failed")
  FAILED,

  @JsonProperty("rejected")
  REJECTED,
}
