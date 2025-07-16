package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.retryIdempotentRequestOnTransientException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException

@Component
class PrisonerSearchClient(@Qualifier("prisonerSearchWebClient") private val webClient: WebClient) {
  fun getPrisoner(prisonerId: String): PrisonerDetails? = try {
    webClient
      .get()
      .uri("/prisoner/{prisonerId}", prisonerId)
      .exchangeToMono { res ->
        when (res.statusCode()) {
          HttpStatus.NOT_FOUND -> Mono.empty()
          HttpStatus.OK -> res.bodyToMono<PrisonerDetails>()
          else -> res.createError()
        }
      }
      .retryIdempotentRequestOnTransientException()
      .block()
  } catch (e: Exception) {
    throw DownstreamServiceException("Get prisoner request failed", e)
  }

  fun findPrisonerDetails(prisonNumbers: Set<String>): List<PrisonerDetails> = webClient
    .post()
    .uri("/prisoner-search/prisoner-numbers")
    .bodyValue(PrisonerNumbers(prisonNumbers))
    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
    .retrieve()
    .bodyToMono<List<PrisonerDetails>>()
    .retryIdempotentRequestOnTransientException()
    .block()!!
}

data class PrisonerNumbers(
  val prisonerNumbers: Set<String>,
) {
  companion object {
    const val PATTERN: String = "\\w\\d{4}\\w{2}"
    val regex: Regex = $$"^$$PATTERN$".toRegex()
  }
}

data class PrisonerDetails(
  val prisonerNumber: String,
  val firstName: String,
  val lastName: String,
  val prisonId: String?,
  val status: String,
  val restrictedPatient: Boolean,
  val cellLocation: String?,
  val supportingPrisonId: String?,
)
