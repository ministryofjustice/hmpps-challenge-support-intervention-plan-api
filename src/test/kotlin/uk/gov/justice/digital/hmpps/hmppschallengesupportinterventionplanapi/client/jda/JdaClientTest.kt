package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.JdaDequeueResponseStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.JdaMockServer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JustifyingSpan
import java.time.OffsetDateTime
import java.util.UUID

class JdaClientTest {
  private lateinit var client: JdaClient

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    client = JdaClient(webClient)
  }

  @Test
  fun `getCaseNoteAnnotationsFromQueue - success`() {
    server.stubDequeueResponse()

    val result = client.getCaseNoteAnnotationsFromQueue()

    assertThat(result).isNotNull
    assertThat(result?.requestId).isEqualTo(UUID.fromString("f091bc73-4f88-4ff6-9e50-5148d29ed3f6"))
    assertThat(result?.correlationId).isEqualTo(UUID.fromString("f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb"))
    assertThat(result?.prompt?.key).isEqualTo("case-note-analysis")
    assertThat(result?.prompt?.version).isEqualTo(3)
    assertThat(result?.status).isEqualTo(JdaDequeueResponseStatus.SUCCEEDED)
    assertThat(result?.responseData).hasSize(1)
    assertThat(result?.responseData?.first()?.caseNoteId).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    assertThat(result?.responseData?.first()?.confidenceLevel).isEqualTo(ConfidenceLevel.HIGH)
    assertThat(result?.responseData?.first()?.justifyingSpans).containsExactly(
      JustifyingSpan(
        text = "annotated text",
        justifies = BehaviourType.PROTECTIVE_FACTORS,
      ),
    )
    assertThat(result?.metadata?.completedAt).isEqualTo(OffsetDateTime.parse("2026-06-27T09:55:03Z"))
    assertThat(result?.metadata?.completionMs).isEqualTo(1200)

    server.verify(exactly(1), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `getCaseNoteAnnotationsFromQueue - not found returns null`() {
    server.stubDequeueResponseNotFound()

    val result = client.getCaseNoteAnnotationsFromQueue()

    assertThat(result).isNull()
    server.verify(exactly(1), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `getCaseNoteAnnotationsFromQueue - downstream service exception`() {
    server.stubDequeueResponseException()

    val exception = assertThrows<DownstreamServiceException> { client.getCaseNoteAnnotationsFromQueue() }

    assertThat(exception.message).isEqualTo("Get case note annotations from queue failed")
    assertThat(exception.cause).isInstanceOf(WebClientResponseException::class.java)
    server.verify(exactly(4), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `submitRequest - success returns response with annotations`() {
    val correlationId = "f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb"
    server.stubSubmitRequest()

    val request = JdaRequest(
      correlationId = correlationId,
      prompt = JdaPrompt(key = "case-note-analysis", version = 0),
      requestData = emptyList<String>(),
    )

    val result = client.submitRequest(request)

    assertThat(result.requestId).isNotNull()
    assertThat(result.correlationId.toString()).isEqualTo(correlationId)
    assertThat(result.prompt.key).isEqualTo("case-note-analysis")
    assertThat(result.status).isEqualTo(JdaRequestStatus.SUCCEEDED)
    assertThat(result.responseData).hasSize(1)

    server.verify(exactly(1), postRequestedFor(urlEqualTo("/v1/submitrequest")))
  }

  @Test
  fun `submitRequest - posts exactly once on success`() {
    val correlationId = "f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb"
    server.stubSubmitRequest()

    val request = JdaRequest(
      correlationId = correlationId,
      prompt = JdaPrompt(key = "case-note-analysis", version = 0),
      requestData = listOf("case note"),
    )

    client.submitRequest(request)

    server.verify(exactly(1), postRequestedFor(urlEqualTo("/v1/submitrequest")))
  }

  @Test
  fun `submitRequest - downstream service exception`() {
    val correlationId = "f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb"
    server.stubSubmitRequestException()

    val request = JdaRequest(
      correlationId = correlationId,
      prompt = JdaPrompt(key = "case-note-analysis", version = 0),
      requestData = emptyList<String>(),
    )

    val exception = assertThrows<DownstreamServiceException> { client.submitRequest(request) }

    assertThat(exception.message).isEqualTo("Submit case notes request failed")
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/v1/submitrequest")))
  }

  @Test
  fun `queueRequest - successful accepted response`() {
    val correlationId = "f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb"
    server.stubQueueRequestAccepted()

    val request = JdaRequest(
      correlationId = correlationId,
      prompt = JdaPrompt(key = "case-note-analysis", version = 0),
      requestData = listOf("case note"),
    )

    client.queueRequest(request)

    server.verify(exactly(1), postRequestedFor(urlEqualTo("/v1/queuerequest")))
  }

  @Test
  fun `queueRequest - downstream failure`() {
    val correlationId = "f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb"
    server.stubQueueRequestException()

    val request = JdaRequest(
      correlationId = correlationId,
      prompt = JdaPrompt(key = "case-note-analysis", version = 0),
      requestData = emptyList<String>(),
    )

    val exception = assertThrows<DownstreamServiceException> { client.queueRequest(request) }

    assertThat(exception.message).isEqualTo("Queue JDA request failed")
    assertThat(exception.cause).isInstanceOf(WebClientResponseException::class.java)
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/v1/queuerequest")))
  }

  companion object {
    @JvmField
    internal val server = JdaMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      server.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      server.stop()
    }
  }
}
