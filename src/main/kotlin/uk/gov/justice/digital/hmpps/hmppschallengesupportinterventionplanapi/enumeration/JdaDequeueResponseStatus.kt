package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.STRING)
enum class JdaDequeueResponseStatus(private val value: String) {
  SUCCEEDED("succeeded"),
  FAILED("failed"),
  REJECTED("rejected"),
  ;

  fun getValue(): String = value

  override fun toString(): String = value
}
