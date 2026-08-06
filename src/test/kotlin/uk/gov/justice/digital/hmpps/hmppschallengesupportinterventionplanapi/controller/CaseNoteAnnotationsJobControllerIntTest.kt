package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.JdaMockServer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNoteAnnotationsService
import java.time.Duration.ofSeconds

class CaseNoteAnnotationsJobControllerIntTest : IntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var caseNoteAnnotationsService: CaseNoteAnnotationsService

  @BeforeEach
  fun resetMocks() {
    jdaServer.resetRequests()
  }

  @Test
  fun `successfully processes all available case note annotations from queue`() {
    jdaServer.stubDequeueResponseThenNotFound()

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().isOk

    await withPollDelay ofSeconds(1) untilCallTo { dequeueRequestCount() } matches { it == 2 }

    verify(caseNoteAnnotationsService, times(1)).getCaseNoteAnnotationsFromQueue()
    jdaServer.verify(exactly(2), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `handles empty queue gracefully`() {
    jdaServer.stubDequeueResponseNotFound()

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().isOk

    await withPollDelay ofSeconds(1) untilCallTo { dequeueRequestCount() } matches { it == 1 }

    verify(caseNoteAnnotationsService, times(1)).getCaseNoteAnnotationsFromQueue()
    jdaServer.verify(exactly(1), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `fails when downstream service denies access`() {
    jdaServer.stubDequeueResponseUnauthorized()

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().is5xxServerError

    await withPollDelay ofSeconds(1) untilCallTo { dequeueRequestCount() } matches { it == 1 }

    verify(caseNoteAnnotationsService, times(1)).getCaseNoteAnnotationsFromQueue()
    jdaServer.verify(exactly(1), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `retries on transient failures from downstream service`() {
    jdaServer.stubDequeueResponseException()

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().is5xxServerError

    await withPollDelay ofSeconds(1) untilCallTo { dequeueRequestCount() } matches { it == 4 }

    verify(caseNoteAnnotationsService, times(1)).getCaseNoteAnnotationsFromQueue()
    jdaServer.verify(exactly(4), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  private fun dequeueRequestCount() = jdaServer.findAll(getRequestedFor(urlEqualTo("/v1/dequeueresponse"))).size

  companion object {
    @JvmField
    internal val jdaServer = JdaMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      jdaServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      jdaServer.stop()
    }
  }
}
