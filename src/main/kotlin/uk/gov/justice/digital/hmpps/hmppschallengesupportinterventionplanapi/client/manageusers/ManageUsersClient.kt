package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.manageusers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.manageusers.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.retryIdempotentRequestOnTransientException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException

@Component
class ManageUsersClient(@Qualifier("manageUsersWebClient") private val webClient: WebClient) {
  fun getUserDetails(username: String): UserDetailsDto? {
    return try {
      webClient
        .get()
        .uri("/users/{username}", username)
        .exchangeToMono { res ->
          when (res.statusCode()) {
            HttpStatus.NOT_FOUND -> Mono.empty()
            HttpStatus.OK -> res.bodyToMono<UserDetailsDto>()
            else -> res.createError()
          }
        }
        .retryIdempotentRequestOnTransientException()
        .block()
    } catch (e: Exception) {
      throw DownstreamServiceException("Get user details request failed", e)
    }
  }
}
