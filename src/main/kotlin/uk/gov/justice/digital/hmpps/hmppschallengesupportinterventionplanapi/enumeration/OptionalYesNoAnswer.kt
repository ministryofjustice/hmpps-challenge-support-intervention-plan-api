package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  type = "String",
  allowableValues = [
    "yes",
    "no",
    "do-not-know",
  ],
)
enum class OptionalYesNoAnswer(@JsonProperty val code: String) {
  YES("yes"),
  NO("no"),
  DO_NOT_KNOW("do-not-know"),
}
