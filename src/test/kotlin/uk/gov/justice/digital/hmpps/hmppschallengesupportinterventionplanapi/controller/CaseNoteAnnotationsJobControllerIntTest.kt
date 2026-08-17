package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
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
import java.time.OffsetDateTime
import java.util.UUID

class CaseNoteAnnotationsJobControllerIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var caseNoteAnnotationRepository: CaseNoteAnnotationRepository

  @BeforeEach
  fun resetMocks() {
    jdaServer.resetAll()
  }

  @Test
  fun `successfully processes all available case note annotations from queue`() {
    jdaServer.stubDequeueResponseThenNotFound()

    webTestClient.post()
      .uri("/queue/case-note-annotations")
      .exchange()
      .expectStatus().isOk

    await withPollDelay ofSeconds(1) untilCallTo { numberOfDequeueRequestsMade() } matches { it == 2 }
    jdaServer.verify(exactly(2), getRequestedFor(urlEqualTo("/v1/dequeueresponse")))
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

    await withPollDelay ofSeconds(1) untilCallTo {
      caseNoteAnnotationRepository.findAll().count { it.requestId == requestId }
    } matches { it == 3 }

    await withPollDelay ofSeconds(1) untilCallTo { numberOfDequeueRequestsMade() } matches { it == 2 }

    val savedAnnotations = caseNoteAnnotationRepository.findAll().filter { it.requestId == requestId }

    assertThat(savedAnnotations).allSatisfy {
      assertThat(it.createdDate).isNotNull()
      assertThat(it.prisonerNumber).isEqualTo(record.prisonNumber)
      assertThat(it.requestId).isEqualTo(requestId)
    }

    assertThat(savedAnnotations)
      .extracting(
        CaseNoteAnnotation::requestId,
        CaseNoteAnnotation::prisonerNumber,
        CaseNoteAnnotation::caseNoteId,
        CaseNoteAnnotation::promptKey,
        CaseNoteAnnotation::promptVersion,
        CaseNoteAnnotation::behaviourType,
        CaseNoteAnnotation::confidenceLevel,
        CaseNoteAnnotation::annotatedText,
      )
      .containsExactlyInAnyOrder(
        tuple(
          requestId,
          record.prisonNumber,
          UUID.fromString("11111111-1111-1111-1111-111111111111"),
          "case-note-analysis",
          3,
          BehaviourType.PROTECTIVE_FACTORS,
          ConfidenceLevel.HIGH,
          "annotated text 1",
        ),
        tuple(
          requestId,
          record.prisonNumber,
          UUID.fromString("11111111-1111-1111-1111-111111111111"),
          "case-note-analysis",
          3,
          BehaviourType.RISKS_AND_TRIGGERS,
          ConfidenceLevel.HIGH,
          "annotated text 2",
        ),
        tuple(
          requestId,
          record.prisonNumber,
          UUID.fromString("11111111-1111-1111-1111-111111111112"),
          "case-note-analysis",
          3,
          BehaviourType.USUAL_BEHAVIOUR_PRESENTATION,
          ConfidenceLevel.LOW,
          "annotated text 3",
        ),
      )
  }

  private fun numberOfDequeueRequestsMade() = jdaServer.findAll(getRequestedFor(urlEqualTo("/v1/dequeueresponse"))).size

  private fun createCaseNoteDequeueResponse(correlationId: UUID, requestId: UUID) = JdaDequeueResponse(
    requestId = requestId,
    correlationId = correlationId,
    prompt = JdaPrompt(
      key = "case-note-analysis",
      version = 3,
    ),
    status = JdaDequeueResponseStatus.SUCCEEDED,
    responseData = listOf(
      JdaDequeueResponseData(
        caseNoteId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        confidenceLevel = ConfidenceLevel.HIGH,
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
        confidenceLevel = ConfidenceLevel.LOW,
        justifyingSpans = listOf(
          JustifyingSpan(
            text = "annotated text 3",
            justifies = BehaviourType.USUAL_BEHAVIOUR_PRESENTATION,
          ),
        ),
      ),
    ),
    metadata = JdaDequeueResponseMetadata(
      requestType = JdaRequestType.ASYNC,
      completedAt = OffsetDateTime.parse("2026-06-27T09:55:03Z"),
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
