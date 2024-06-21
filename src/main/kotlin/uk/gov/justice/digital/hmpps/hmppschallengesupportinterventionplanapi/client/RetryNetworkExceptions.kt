package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client

import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.io.IOException
import java.time.Duration

fun <T> Mono<T>.retryIdempotentRequestOnTransientException(): Mono<T> =
  retryWhen(
    Retry.backoff(3, Duration.ofMillis(250))
      .filter {
        it is IOException || (it is WebClientResponseException && it.statusCode.is5xxServerError)
      }.onRetryExhaustedThrow { _, signal ->
        signal.failure()
      },
  )
