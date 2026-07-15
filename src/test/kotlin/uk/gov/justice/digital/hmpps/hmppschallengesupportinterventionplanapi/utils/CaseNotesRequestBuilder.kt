package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNotesTypeSubType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class TypeSubTypeJson(
  val type: String,
  val subTypes: List<String>,
)

class CaseNotesRequestBuilder {
  var includeSensitive: Boolean = true
  var typeSubTypes: List<TypeSubTypeJson> = listOf(TypeSubTypeJson(type = "string", subTypes = listOf("string")))
  var occurredFrom: LocalDateTime = LocalDateTime.parse("2026-07-13T08:43:27")
  var occurredTo: LocalDateTime = LocalDateTime.parse("2026-07-13T08:43:27")
  var page: Int = 1
  var size: Int = 1
  var sort: String = "string"

  fun withIncludeSensitive(value: Boolean) = apply { includeSensitive = value }

  fun withTypeSubTypes(value: List<TypeSubTypeJson>) = apply { typeSubTypes = value }

  fun withOccurredFrom(value: LocalDateTime) = apply { occurredFrom = value }

  fun withOccurredTo(value: LocalDateTime) = apply { occurredTo = value }

  fun withPage(value: Int) = apply { page = value }

  fun withSize(value: Int) = apply { size = value }

  fun withSort(value: String) = apply { sort = value }

  fun buildRequest(): CaseNotesRequest {
    return CaseNotesRequest(
      includeSensitive = includeSensitive,
      typeSubTypes = typeSubTypes.map { CaseNotesTypeSubType(type = it.type, subTypes = it.subTypes) },
      occurredFrom = occurredFrom,
      occurredTo = occurredTo,
      page = page,
      size = size,
      sort = sort,
    )
  }

  fun build(): String {
    val requestDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val typeSubTypesJson = typeSubTypes.joinToString(",\n") { item ->
      val subTypesJson = item.subTypes.joinToString(",\n") { subType -> "\"$subType\"" }
      """
      {
        "type": "${item.type}",
        "subTypes": [
          $subTypesJson
        ]
      }
      """.trimIndent()
    }

    return """
      {
        "includeSensitive": $includeSensitive,
        "typeSubTypes": [
          $typeSubTypesJson
        ],
        "occurredFrom": "${occurredFrom.format(requestDateTimeFormatter)}",
        "occurredTo": "${occurredTo.format(requestDateTimeFormatter)}",
        "page": $page,
        "size": $size,
        "sort": "$sort"
      }
    """.trimIndent()
  }
}
