package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.DownstreamServiceException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.OFFENDER_IDENTIFIER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.OFFENDER_IDENTIFIER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.OFFENDER_IDENTIFIER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.CaseNotesServer
import java.time.LocalDateTime
import java.util.UUID

class CaseNotesClientTest {
  private lateinit var client: CaseNotesClient
  private val request = CaseNotesRequest(
    includeSensitive = true,
    typeSubTypes = listOf(
      CaseNotesTypeSubType(
        type = "string",
        subTypes = listOf("string"),
      ),
    ),
    occurredFrom = LocalDateTime.parse("2026-07-13T08:43:27.935"),
    occurredTo = LocalDateTime.parse("2026-07-13T08:43:27.935"),
    page = 1,
    size = 1,
    sort = "string",
  )
  private val requestJson = """
    {
      "includeSensitive": true,
      "typeSubTypes": [
        {
          "type": "string",
          "subTypes": [
            "string"
          ]
        }
      ],
      "occurredFrom": "2026-07-13T08:43:27.935",
      "occurredTo": "2026-07-13T08:43:27.935",
      "page": 1,
      "size": 1,
      "sort": "string"
    }
  """.trimIndent()

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    client = CaseNotesClient(webClient)
  }

  @Test
  fun `getCaseNotes - success`() {
    server.stubGetCaseNotes()

    val result = client.getCaseNotes(OFFENDER_IDENTIFIER, request)

    assertThat(result).isEqualTo(
      CaseNotesResponse(
        content = listOf(
          CaseNote(
            caseNoteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
            offenderIdentifier = OFFENDER_IDENTIFIER,
            type = "TYPE",
            typeDescription = "Type description",
            subType = "SUB_TYPE",
            subTypeDescription = "Subtype description",
            creationDateTime = LocalDateTime.parse("2026-07-09T12:00:00"),
            occurrenceDateTime = LocalDateTime.parse("2026-07-09T11:30:00"),
            authorName = "Author Name",
            authorUserId = "USER123",
            authorUsername = "author.username",
            text = "Some case note text",
            locationId = "LEI",
            sensitive = false,
            amendments = listOf(
              CaseNoteAmendment(
                creationDateTime = LocalDateTime.parse("2026-07-09T12:05:00"),
                authorUserName = "author.username",
                authorName = "Author Name",
                authorUserId = "USER123",
                additionalNoteText = "Amendment text",
                id = UUID.fromString("223e4567-e89b-12d3-a456-426614174000"),
              ),
            ),
          ),
        ),
        hasCaseNotes = true,
      ),
    )
  }

  @Test
  fun `getCaseNotes - sends correct post body`() {
    server.stubGetCaseNotes()

    client.getCaseNotes(OFFENDER_IDENTIFIER, request)

    server.verify(
      exactly(1),
      postRequestedFor(urlEqualTo("/search/case-notes/$OFFENDER_IDENTIFIER"))
        .withRequestBody(equalToJson(requestJson)),
    )
  }

  @Test
  fun `getCaseNotes - case notes not found`() {
    val result = client.getCaseNotes(OFFENDER_IDENTIFIER_NOT_FOUND, request)

    assertThat(result).isNull()
    server.verify(
      exactly(1),
      postRequestedFor(urlEqualTo("/search/case-notes/$OFFENDER_IDENTIFIER_NOT_FOUND"))
        .withRequestBody(equalToJson(requestJson)),
    )
  }

  @Test
  fun `getCaseNotes - downstream service exception`() {
    server.stubGetCaseNotesException()

    val exception = assertThrows<DownstreamServiceException> { client.getCaseNotes(OFFENDER_IDENTIFIER_THROW_EXCEPTION, request) }
    assertThat(exception.message).isEqualTo("Get case notes request failed")
    with(exception.cause) {
      assertThat(this).isInstanceOf(WebClientResponseException::class.java)
      assertThat(this!!.message).isEqualTo("500 Internal Server Error from POST http://localhost:8113/search/case-notes/$OFFENDER_IDENTIFIER_THROW_EXCEPTION")
    }
    server.verify(
      exactly(4),
      postRequestedFor(urlEqualTo("/search/case-notes/$OFFENDER_IDENTIFIER_THROW_EXCEPTION"))
        .withRequestBody(equalToJson(requestJson)),
    )
  }

  companion object {
    @JvmField
    internal val server = CaseNotesServer()

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
