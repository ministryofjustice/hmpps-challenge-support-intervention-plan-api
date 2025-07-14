package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerDetails

internal const val PRISON_NUMBER = "A1234AA"
internal const val PRISON_NUMBER_NOT_FOUND = "NOT_FOUND"
internal const val PRISON_NUMBER_THROW_EXCEPTION = "THROW"

class PrisonerSearchServer : WireMockServer(8112) {
  private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  fun stubGetPrisoners(details: List<PrisonerDetails>): StubMapping = stubFor(
    post(urlPathEqualTo("/prisoner-search/prisoner-numbers"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(details))
          .withStatus(200),
      ),
  )

  fun stubGetPrisoner(prisonerDetails: PrisonerDetails): StubMapping = stubFor(
    get("/prisoner/${prisonerDetails.prisonerNumber}")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(prisonerDetails))
          .withStatus(200),
      ),
  )

  fun stubGetPrisoner(prisonNumber: String = PRISON_NUMBER): StubMapping = stubFor(
    get("/prisoner/$prisonNumber")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              PrisonerDetails(
                prisonerNumber = prisonNumber,
                "First",
                "Last",
                PRISON_CODE_LEEDS,
                "ACTIVE IN",
                false,
                "A-1-002",
                null,
              ),
            ),
          )
          .withStatus(200),
      ),
  )

  fun stubGetPrisonerException(prisonNumber: String = PRISON_NUMBER_THROW_EXCEPTION): StubMapping = stubFor(get("/prisoner/$prisonNumber").willReturn(aResponse().withStatus(500)))

  fun stubGetPrisonerNotFoundException(prisonNumber: String = PRISON_NUMBER_NOT_FOUND): StubMapping = stubFor(get("/prisoner/$prisonNumber").willReturn(aResponse().withStatus(404)))
}

class PrisonerSearchExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonerSearch = PrisonerSearchServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonerSearch.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonerSearch.resetRequests()
    prisonerSearch.stubGetPrisoner()
    prisonerSearch.stubGetPrisonerException()
    prisonerSearch.stubGetPrisonerNotFoundException()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonerSearch.stop()
  }
}
