package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.JdaDequeueResponseStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.JdaMockServer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JustifyingSpan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import java.time.Duration.ofSeconds
import java.time.LocalDateTime
import java.util.UUID

class CaseNoteAnnotationsJobControllerIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var caseNoteAnnotationRepository: CaseNoteAnnotationRepository

  @Autowired
  lateinit var caseNoteAnalysedRepository: CaseNoteAnalysedRepository

  @BeforeEach
  fun resetMocks() {
    jdaServer.resetAll()
    caseNoteAnnotationRepository.deleteAll()
    caseNoteAnalysedRepository.deleteAll()
    jdaServer.stubAcknowledgeDequeueResponseSuccess()
  }

  @Test
  fun `successfully processes all available case note annotations from queue`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val response = createCaseNoteDequeueResponse(correlationId = record.id, requestId = UUID.fromString("f091bc73-4f88-4ff6-9e50-5148d29ed3f6"))

    jdaServer.stubDequeueResponseThenNotFound(jsonMapper.writeValueAsString(response))

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().isOk

    await withPollDelay ofSeconds(1) untilCallTo { numberOfDequeueRequestsMade() } matches { it == 2 }
    jdaServer.verify(exactly(2), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `duplicate delivery converges to one parent and annotation set per case note`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val response = createCaseNoteDequeueResponse(record.id, UUID.randomUUID())

    processSnapshot(response)
    val originalIds = analysedIds(response)
    processSnapshot(response)

    assertThat(analysedIds(response)).containsExactlyElementsOf(originalIds)
    assertThat(caseNoteAnalysedRepository.findAll()).hasSize(2)
    assertThat(caseNoteAnnotationRepository.findAll().map { it.annotatedText })
      .containsExactlyInAnyOrder("annotated text 1", "annotated text 2", "annotated text 3")
    jdaServer.verify(exactly(1), postRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `latest snapshot replaces previous annotations and parent metadata`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val response = createCaseNoteDequeueResponse(record.id, UUID.randomUUID())
    processSnapshot(response)
    val originalIds = analysedIds(response)
    val latest = response.copy(
      requestId = UUID.randomUUID(),
      prompt = JdaPrompt(key = "latest-prompt", version = 4),
      responseData = response.responseData!!.map {
        it.copy(
          risksAndTriggers = 5,
          justifyingSpans = listOf(JustifyingSpan("latest annotation", BehaviourType.RISKS_AND_TRIGGERS)),
        )
      },
    )

    processSnapshot(latest)

    assertThat(analysedIds(latest)).containsExactlyElementsOf(originalIds)
    assertThat(caseNoteAnalysedRepository.findAll()).hasSize(2).allSatisfy {
      assertThat(it.requestId).isEqualTo(latest.requestId)
      assertThat(it.promptKey).isEqualTo("latest-prompt")
      assertThat(it.promptVersion).isEqualTo(4)
      assertThat(it.risksAndTriggersRelevancy).isEqualTo(5)
    }
    assertThat(caseNoteAnnotationRepository.findAll().map { it.annotatedText })
      .containsExactlyInAnyOrder("latest annotation", "latest annotation")
    jdaServer.verify(exactly(1), postRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `failed replacement rolls back the snapshot without acknowledgement and can be retried`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val response = createCaseNoteDequeueResponse(record.id, UUID.randomUUID())
    processSnapshot(response)
    val originalIds = analysedIds(response)
    val latest = response.copy(
      requestId = UUID.randomUUID(),
      responseData = response.responseData!!.map {
        it.copy(justifyingSpans = listOf(JustifyingSpan("latest annotation", BehaviourType.RISKS_AND_TRIGGERS)))
      },
    )
    val invalid = latest.copy(
      prompt = JdaPrompt(key = "x".repeat(256), version = 4),
    )
    jdaServer.resetAll()
    jdaServer.stubAcknowledgeDequeueResponseSuccess()
    jdaServer.stubDequeueResponseThenNotFound(jsonMapper.writeValueAsString(invalid))

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().is5xxServerError

    assertThat(analysedIds(response)).containsExactlyElementsOf(originalIds)
    assertThat(caseNoteAnalysedRepository.findAll()).allSatisfy {
      assertThat(it.requestId).isEqualTo(response.requestId)
    }
    assertThat(caseNoteAnnotationRepository.findAll().map { it.annotatedText })
      .containsExactlyInAnyOrder("annotated text 1", "annotated text 2", "annotated text 3")
    jdaServer.verify(exactly(0), postRequestedFor(urlEqualTo("/v1/dequeueresponse")))
    jdaServer.verify(exactly(1), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))

    processSnapshot(latest)

    assertThat(analysedIds(latest)).containsExactlyElementsOf(originalIds)
    assertThat(caseNoteAnnotationRepository.findAll().map { it.annotatedText })
      .containsExactlyInAnyOrder("latest annotation", "latest annotation")
    jdaServer.verify(exactly(1), postRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  private fun processSnapshot(response: JdaDequeueResponse) {
    jdaServer.resetAll()
    jdaServer.stubAcknowledgeDequeueResponseSuccess()
    jdaServer.stubDequeueResponseThenNotFound(jsonMapper.writeValueAsString(response))
    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().isOk
  }

  private fun analysedIds(response: JdaDequeueResponse): List<UUID> = response.responseData!!.map { data ->
    requireNotNull(caseNoteAnalysedRepository.findByInvestigationIdAndCaseNoteId(response.correlationId, data.caseNoteId)).id
  }

  @Test
  fun `handles empty queue gracefully`() {
    jdaServer.stubDequeueResponseNotFound()

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().isOk

    await withPollDelay ofSeconds(1) untilCallTo { numberOfDequeueRequestsMade() } matches { it == 1 }
    jdaServer.verify(exactly(1), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `fails when downstream service denies access`() {
    jdaServer.stubDequeueResponseUnauthorized()

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().is5xxServerError

    await withPollDelay ofSeconds(1) untilCallTo { numberOfDequeueRequestsMade() } matches { it == 1 }
    jdaServer.verify(exactly(1), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `retries on transient failures from downstream service`() {
    jdaServer.stubDequeueResponseException()

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().is5xxServerError

    await withPollDelay ofSeconds(1) untilCallTo { numberOfDequeueRequestsMade() } matches { it == 4 }
    jdaServer.verify(exactly(4), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
  }

  @Test
  fun `persists case note annotation rows and their content to the database`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())
    val requestId = UUID.fromString("f091bc73-4f88-4ff6-9e50-5148d29ed3f6")
    val response = createCaseNoteDequeueResponse(correlationId = record.id, requestId = requestId)

    jdaServer.stubDequeueResponseThenNotFound(jsonMapper.writeValueAsString(response))

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().isOk

    await withPollDelay ofSeconds(1) untilCallTo { numberOfDequeueRequestsMade() } matches { it == 2 }
  }

  private fun numberOfDequeueRequestsMade() = jdaServer.findAll(getRequestedFor(urlEqualTo("/v1/dequeueresponse"))).size

  private fun createCaseNoteDequeueResponse(correlationId: UUID, requestId: UUID) = JdaDequeueResponse(
    requestId = requestId,
    correlationId = correlationId,
    receiptId = "receipt-${UUID.randomUUID()}",
    prompt = JdaPrompt(
      key = "case-note-analysis",
      version = 3,
    ),
    status = JdaDequeueResponseStatus.SUCCEEDED,
    responseData = listOf(
      JdaDequeueResponseData(
        caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        usualBehaviourPresentation = 3,
        risksAndTriggers = 2,
        protectiveFactors = 4,
        justifyingSpans = listOf(
          JustifyingSpan(
            text = "annotated text 1",
            justifies = BehaviourType.PROTECTIVE_FACTORS,
          ),
          JustifyingSpan(
            text = "annotated text 2",
            justifies = BehaviourType.RISKS_AND_TRIGGERS,
          ),
        ),
      ),
      JdaDequeueResponseData(
        caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111112"),
        usualBehaviourPresentation = 1,
        risksAndTriggers = 0,
        protectiveFactors = 0,
        justifyingSpans = listOf(
          JustifyingSpan(
            text = "annotated text 3",
            justifies = BehaviourType.USUAL_BEHAVIOUR_PRESENTATION,
          ),
        ),
      ),
    ),
    metaData = JdaDequeueResponseMetadata(
      requestType = JdaRequestType.ASYNC,
      completedAt = LocalDateTime.parse("2026-06-27T09:55:03"),
      completionMs = 1200,
    ),
  )

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
