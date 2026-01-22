package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

@JacksonComponent
class StringDeserializer : ValueDeserializer<String>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String = p.string.filter { it != Char.MIN_VALUE }
}
