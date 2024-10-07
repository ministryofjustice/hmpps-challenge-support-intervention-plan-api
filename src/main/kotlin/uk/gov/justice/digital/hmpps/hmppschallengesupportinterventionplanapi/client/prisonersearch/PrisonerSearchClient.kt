package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.retryIdempotentRequestOnTransientException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException

@Component
class PrisonerSearchClient(@Qualifier("prisonerSearchWebClient") private val webClient: WebClient) {
  fun getPrisoner(prisonerId: String): PrisonerDto? {
    return try {
      webClient
        .get()
        .uri("/prisoner/{prisonerId}", prisonerId)
        .exchangeToMono { res ->
          when (res.statusCode()) {
            HttpStatus.NOT_FOUND -> Mono.empty()
            HttpStatus.OK -> res.bodyToMono<PrisonerDto>()
            else -> res.createError()
          }
        }
        .retryIdempotentRequestOnTransientException()
        .block()
    } catch (e: Exception) {
      throw DownstreamServiceException("Get prisoner request failed", e)
    }
  }
}
