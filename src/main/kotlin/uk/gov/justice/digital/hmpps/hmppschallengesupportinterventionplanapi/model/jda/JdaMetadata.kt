package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class JdaMetadata(
  val requestType: JdaRequestType,
  @get:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val submittedAt: OffsetDateTime,
) {
  companion object {
    @JsonCreator
    @JvmStatic
    fun from(
      @JsonProperty("requestType") requestType: JdaRequestType,
      @JsonProperty("submittedAt") submittedAt: String,
    ) = JdaMetadata(
      requestType = requestType,
      submittedAt = OffsetDateTime.parse(submittedAt),
    )
  }
}
