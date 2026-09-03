package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import com.fasterxml.jackson.annotation.JsonProperty

enum class JdaDequeueResponseStatus {
  @JsonProperty("succeeded")
  SUCCEEDED,

  @JsonProperty("failed")
  FAILED,

  @JsonProperty("rejected")
  REJECTED,
}
