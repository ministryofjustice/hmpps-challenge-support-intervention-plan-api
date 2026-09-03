package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.STRING)
enum class JdaRequestType(private val value: String) {
  SYNC("sync"),
  ASYNC("async"),
  ;

  fun getValue(): String = value
  override fun toString(): String = value
}
