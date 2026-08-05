package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.JdaMockServer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestType
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

    assertThat(result).isEqualTo(
      JdaDequeueResponse(
        requestId = UUID.fromString("f091bc73-4f88-4ff6-9e50-5148d29ed3f6"),
        correlationId = "f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb",
        prompt = JdaPrompt.caseNoteAnalysis(),
        status = JdaDequeueResponseStatus.SUCCEEDED,
        responseData = JdaDequeueResponseData(
          todo = "todo",
        ),
        metadata = JdaDequeueResponseMetadata(
          requestType = JdaRequestType.ASYNC,
          completedAt = OffsetDateTime.parse("2026-06-27T09:55:03Z"),
          completionMs = 1200,
        ),
      ),
    )

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
