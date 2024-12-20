package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch

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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PrisonerSearchServer

class PrisonerDetailsSearchClientTest {
  private lateinit var client: PrisonerSearchClient

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    client = PrisonerSearchClient(webClient)
  }

  @Test
  fun `getPrisoner - success`() {
    server.stubGetPrisoner()

    val result = client.getPrisoner(PRISON_NUMBER)

    assertThat(result!!).isEqualTo(
      PrisonerDetails(
        prisonerNumber = PRISON_NUMBER,
        "First",
        "Last",
        PRISON_CODE_LEEDS,
        "ACTIVE IN",
        false,
        "A-1-002",
        null,
      ),
    )
    server.verify(exactly(1), getRequestedFor(urlEqualTo("/prisoner/$PRISON_NUMBER")))
  }

  @Test
  fun `getPrisoner - prisoner not found`() {
    val result = client.getPrisoner(PRISON_NUMBER_NOT_FOUND)

    assertThat(result).isNull()
    server.verify(exactly(1), getRequestedFor(urlEqualTo("/prisoner/$PRISON_NUMBER_NOT_FOUND")))
  }

  @Test
  fun `getPrisoner - downstream service exception`() {
    server.stubGetPrisonerException()

    val exception = assertThrows<DownstreamServiceException> { client.getPrisoner(PRISON_NUMBER_THROW_EXCEPTION) }
    assertThat(exception.message).isEqualTo("Get prisoner request failed")
    with(exception.cause) {
      assertThat(this).isInstanceOf(WebClientResponseException::class.java)
      assertThat(this!!.message).isEqualTo("500 Internal Server Error from GET http://localhost:8112/prisoner/${PRISON_NUMBER_THROW_EXCEPTION}")
    }
    server.verify(exactly(4), getRequestedFor(urlEqualTo("/prisoner/$PRISON_NUMBER_THROW_EXCEPTION")))
  }

  companion object {
    @JvmField
    internal val server = PrisonerSearchServer()

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
