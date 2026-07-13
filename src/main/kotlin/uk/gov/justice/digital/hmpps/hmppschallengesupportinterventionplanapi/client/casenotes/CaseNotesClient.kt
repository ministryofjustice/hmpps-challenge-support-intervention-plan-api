package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.retryIdempotentRequestOnTransientException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException
import java.time.LocalDateTime
import java.util.UUID

@Component
class CaseNotesClient(@Qualifier("caseNotesWebClient") private val webClient: WebClient) {
  fun getCaseNotes(offenderIdentifier: String, request: CaseNotesRequest): CaseNotesResponse? = try {
    webClient
      .post()
      .uri("/search/case-notes/{offenderIdentifier}", offenderIdentifier)
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchangeToMono { res ->
        when (res.statusCode()) {
          HttpStatus.NOT_FOUND -> Mono.empty()
          HttpStatus.OK -> res.bodyToMono<CaseNotesResponse>()
          else -> res.createError()
        }
      }
      .retryIdempotentRequestOnTransientException()
      .block()
  } catch (e: Exception) {
    throw DownstreamServiceException("Get case notes request failed", e)
  }
}

data class CaseNotesRequest(
  val includeSensitive: Boolean,
  val typeSubTypes: List<CaseNotesTypeSubType>,
  val occurredFrom: LocalDateTime,
  val occurredTo: LocalDateTime,
  val page: Int,
  val size: Int,
  val sort: String,
)

data class CaseNotesTypeSubType(
  val type: String,
  val subTypes: List<String>,
)

data class CaseNotesResponse(
  val content: List<CaseNote>,
  val hasCaseNotes: Boolean,
)

data class CaseNote(
  val caseNoteId: UUID,
  val offenderIdentifier: String,
  val type: String,
  val typeDescription: String,
  val subType: String,
  val subTypeDescription: String,
  val creationDateTime: LocalDateTime,
  val occurrenceDateTime: LocalDateTime,
  val authorName: String,
  val authorUserId: String,
  val authorUsername: String,
  val text: String,
  val locationId: String,
  val sensitive: Boolean,
  val amendments: List<CaseNoteAmendment>,
)

data class CaseNoteAmendment(
  val creationDateTime: LocalDateTime,
  val authorUserName: String,
  val authorName: String,
  val authorUserId: String?,
  val additionalNoteText: String,
  val id: UUID,
)
