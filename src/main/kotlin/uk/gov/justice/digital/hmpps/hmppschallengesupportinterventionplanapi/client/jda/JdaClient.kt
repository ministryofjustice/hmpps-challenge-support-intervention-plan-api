package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestResponse

@Component
class JdaClient(
  @Qualifier("jdaWebClient")
  private val webClient: WebClient,
) {

  fun <T> submitRequest(
    request: JdaRequest<T>,
  ): JdaRequestResponse = TODO("Implemented in JDA-361")

  fun <T> queueRequest(
    request: JdaRequest<T>,
  ): JdaRequestResponse = TODO("Future asynchronous JDA integration")
}
