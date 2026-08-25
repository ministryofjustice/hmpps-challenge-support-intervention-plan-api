package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.retryIdempotentRequestOnTransientException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestResponse

@Component
class JdaClient(
  @Qualifier("jdaWebClient")
  private val webClient: WebClient,
) {

  fun <T> submitRequest(
    request: JdaRequest<T>,
  ): JdaRequestResponse = try {
    webClient
      .post()
      .uri("/v1/submitrequest")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchangeToMono { res ->
        when (res.statusCode()) {
          HttpStatus.OK -> res.bodyToMono<JdaRequestResponse>()
          else -> res.createError()
        }
      }
      .block()
      ?: throw DownstreamServiceException("Submit request returned null response", RuntimeException("No response body"))
  } catch (e: Exception) {
    throw DownstreamServiceException("Submit case notes request failed", e)
  }

  fun <T> queueRequest(
    request: JdaRequest<T>,
  ): JdaRequestResponse = TODO("Future asynchronous JDA integration")

  fun getCaseNoteAnnotationsFromQueue(): JdaDequeueResponse? = try {
    webClient
      .get()
      .uri("/v1/dequeueresponse")
      .accept(MediaType.APPLICATION_JSON)
      .exchangeToMono { res ->
        when (res.statusCode()) {
          HttpStatus.OK -> res.bodyToMono<JdaDequeueResponse>()
          HttpStatus.NOT_FOUND -> Mono.empty()
          else -> res.createError()
        }
      }
      .retryIdempotentRequestOnTransientException()
      .block()
  } catch (e: Exception) {
    throw DownstreamServiceException("Get case note annotations from queue failed", e)
  }
}
