package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import com.fasterxml.jackson.annotation.JsonInclude

data class JdaPrompt(
  val key: String,
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  val version: Int,
)
