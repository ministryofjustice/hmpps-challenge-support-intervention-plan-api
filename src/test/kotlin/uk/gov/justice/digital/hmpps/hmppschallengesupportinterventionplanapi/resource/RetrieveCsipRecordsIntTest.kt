package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CsipSummaries
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import java.time.LocalDateTime

class RetrieveCsipRecordsIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri("/prisoners/csip-records").exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no required role`() {
    val response = webTestClient.post()
      .uri("/prisoners/csip-records")
      .bodyValue(listOf("F1234PN"))
      .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .exchange().errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - when no csip record exists for filter conditions`() {
    val response = getCsipRecordsResponseSpec(setOf(), mapOf()).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure: At least one prison number must be provided")
    }
  }

  @Test
  fun `200 ok - when no csip record exists for filter conditions`() {
    val response = getCsipRecords(setOf("N1234EX"))
    assertThat(response.content).isEmpty()
  }

  @Test
  fun `200 ok - csip records details are correctly returned`() {
    val prisonNumber = "M1234DT"
    val csip = dataSetup(generateCsipRecord(prisonNumber).withReferral().withPlan()) { it }

    with(getCsipRecords(setOf(prisonNumber))) {
      assertThat(content.size).isEqualTo(1)
      val summary = content.first()
      with(summary) {
        assertThat(id).isEqualTo(csip.id)
        assertThat(prisonNumber).isEqualTo(csip.prisonNumber)
        assertThat(logCode).isEqualTo(csip.logCode)
        assertThat(referralDate).isEqualTo(csip.referral!!.referralDate)
        assertThat(nextReviewDate).isEqualTo(csip.plan!!.firstCaseReviewDate)
        assertThat(incidentType.code).isEqualTo(csip.referral!!.incidentType.code)
        assertThat(caseManager).isEqualTo(csip.plan!!.caseManager)
        assertThat(status).isEqualTo(csip.status)
      }
    }
  }

  @Test
  fun `200 ok - csip records matching prison number are returned`() {
    val givenPrisonNumbers = setOf("M1234PN", "M1235PN", "M1236PN")
    givenPrisonNumbers.map { pn ->
      dataSetup(generateCsipRecord(pn).withReferral()) { it }
    }

    with(getCsipRecords(givenPrisonNumbers)) {
      assertThat(content.size).isEqualTo(3)
    }

    with(getCsipRecords(givenPrisonNumbers.take(2).toSet())) {
      assertThat(content.size).isEqualTo(2)
    }
  }

  @Test
  fun `200 ok - csip records matching log code are returned`() {
    val prisonNumber = "M1234LC"
    dataSetup(generateCsipRecord(prisonNumber, logCode = "LEI").withReferral()) { it }
    dataSetup(generateCsipRecord(prisonNumber, logCode = "MDI").withReferral()) { it }

    with(getCsipRecords(setOf(prisonNumber), mapOf("logCode" to "LEI"))) {
      assertThat(content.size).isEqualTo(1)
      assertThat(content.first().prisonNumber).isEqualTo(prisonNumber)
      assertThat(content.first().logCode).isEqualTo("LEI")
    }
  }

  @Test
  fun `200 ok - csip records matching created date are returned`() {
    val tooEarly = "N1234TE"
    val tooLate = "N1234TL"
    val match = "M1234DR"
    dataSetup(generateCsipRecord(tooEarly, createdAt = LocalDateTime.now().minusDays(1)).withReferral()) { it }
    dataSetup(generateCsipRecord(match, createdAt = LocalDateTime.now().minusDays(3)).withReferral()) { it }
    dataSetup(generateCsipRecord(tooLate, createdAt = LocalDateTime.now().minusDays(7)).withReferral()) { it }

    with(
      getCsipRecords(
        setOf(tooLate, match, tooEarly),
        mapOf(
          "createdAtStart" to LocalDateTime.now().minusDays(5).toString(),
          "createdAtEnd" to LocalDateTime.now().minusDays(2).toString(),
        ),
      ),
    ) {
      assertThat(content.size).isEqualTo(1)
      assertThat(content.first().prisonNumber).isEqualTo(match)
    }
  }

  fun getCsipRecordsResponseSpec(prisonNumbers: Set<String>, params: Map<String, String>): WebTestClient.ResponseSpec =
    webTestClient.method(HttpMethod.GET)
      .uri { ub ->
        ub.path("/prisoners/csip-records")
        params.forEach {
          ub.queryParam(it.key, it.value)
        }
        ub.build()
      }
      .bodyValue(prisonNumbers)
      .headers(setAuthorisation(roles = listOfNotNull(ROLE_CSIP_UI)))
      .exchange()

  fun getCsipRecords(prisonNumbers: Set<String>, params: Map<String, String> = mapOf()): CsipSummaries =
    getCsipRecordsResponseSpec(prisonNumbers, params).successResponse()
}
