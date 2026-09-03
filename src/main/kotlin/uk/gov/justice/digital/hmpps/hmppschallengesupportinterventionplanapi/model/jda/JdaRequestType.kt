package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import com.fasterxml.jackson.annotation.JsonProperty

enum class JdaRequestType {
  @JsonProperty("sync")
  SYNC,

  @JsonProperty("async")
  ASYNC,
}
