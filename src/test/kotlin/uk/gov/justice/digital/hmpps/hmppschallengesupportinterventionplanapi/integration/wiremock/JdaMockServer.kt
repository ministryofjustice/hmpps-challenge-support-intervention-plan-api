package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED

class JdaMockServer : WireMockServer(8114) {
  private companion object {
    const val DEQUEUE_SCENARIO = "jda-dequeue"
    const val AFTER_FIRST_DEQUEUE = "after-first-dequeue"
  }

  fun stubDequeueResponse(): StubMapping = stubFor(
    get("/v1/dequeueresponse")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "requestId": "f091bc73-4f88-4ff6-9e50-5148d29ed3f6",
              "correlationId": "f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb",
              "prompt": {
                "key": "case-note-analysis",
                "version": 3
              },
              "status": "succeeded",
              "responseData": {
                "todo": "todo"
              },
              "metadata": {
                "requestType": "async",
                "completedAt": "2026-06-27T09:55:03Z",
                "completionMs": 1200
              }
            }
            """.trimIndent(),
          )
          .withStatus(200),
      ),
  )

  fun stubDequeueResponseThenNotFound() {
    stubFor(
      get("/v1/dequeueresponse")
        .inScenario(DEQUEUE_SCENARIO)
        .whenScenarioStateIs(STARTED)
        .willSetStateTo(AFTER_FIRST_DEQUEUE)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "requestId": "f091bc73-4f88-4ff6-9e50-5148d29ed3f6",
                "correlationId": "f4f7ac6f-1d75-472f-a3a0-f0ee8a33fbbb",
                "prompt": {
                  "key": "case-note-analysis",
                  "version": 3
                },
                "status": "succeeded",
                "responseData": {
                  "todo": "todo"
                },
                "metadata": {
                  "requestType": "async",
                  "completedAt": "2026-06-27T09:55:03Z",
                  "completionMs": 1200
                }
              }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )

    stubFor(
      get("/v1/dequeueresponse")
        .inScenario(DEQUEUE_SCENARIO)
        .whenScenarioStateIs(AFTER_FIRST_DEQUEUE)
        .willReturn(aResponse().withStatus(404)),
    )
  }

  fun stubDequeueResponseNotFound(): StubMapping = stubFor(
    get("/v1/dequeueresponse")
      .willReturn(aResponse().withStatus(404)),
  )

  fun stubDequeueResponseUnauthorized(): StubMapping = stubFor(
    get("/v1/dequeueresponse")
      .willReturn(aResponse().withStatus(401)),
  )

  fun stubDequeueResponseException(): StubMapping = stubFor(
    get("/v1/dequeueresponse")
      .willReturn(aResponse().withStatus(500)),
  )
}
