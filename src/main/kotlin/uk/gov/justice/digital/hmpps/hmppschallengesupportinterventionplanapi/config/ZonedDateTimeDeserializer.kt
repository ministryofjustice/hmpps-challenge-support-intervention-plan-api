package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

val EuropeLondon: ZoneId = ZoneId.of("Europe/London")
fun LocalDateTime.toZoneDateTime(): ZonedDateTime = atZone(ZoneId.systemDefault()).withZoneSameInstant(EuropeLondon)

@JsonComponent
class ZonedDateTimeDeserializer : JsonDeserializer<ZonedDateTime>() {
  companion object {
    private val formatter: DateTimeFormatter = DateTimeFormatterBuilder().parseCaseInsensitive()
      .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      .parseLenient()
      .optionalStart()
      .appendOffsetId()
      .parseStrict()
      .optionalStart()
      .appendLiteral('[')
      .parseCaseSensitive()
      .appendZoneRegionId()
      .appendLiteral(']')
      .optionalEnd()
      .optionalEnd()
      .toFormatter()

    fun deserialize(text: String): ZonedDateTime {
      val datetime = formatter.parseBest(text, ZonedDateTime::from, LocalDateTime::from)
      return if (datetime is ZonedDateTime) {
        datetime.withZoneSameInstant(EuropeLondon)
      } else {
        (datetime as LocalDateTime).toZoneDateTime()
      }
    }
  }

  @Throws(IOException::class, JsonProcessingException::class)
  override fun deserialize(parser: JsonParser, context: DeserializationContext?) = deserialize(parser.text)
}
