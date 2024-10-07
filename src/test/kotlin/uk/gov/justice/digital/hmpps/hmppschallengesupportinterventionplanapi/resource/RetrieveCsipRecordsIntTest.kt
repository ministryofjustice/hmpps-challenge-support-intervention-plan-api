package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.toPersonLocation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSummaries
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import java.time.LocalDateTime

class RetrieveCsipRecordsIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(urlToTest("M1234NA")).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no required role`() {
    val response = webTestClient.get()
      .uri("/prisoners/N123AU/csip-records")
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

  @ParameterizedTest
  @MethodSource("parameterValidations")
  fun `400 bad request - when parameters violate validation restrictions`(
    params: Map<String, String>,
    message: String,
  ) {
    val response = getCsipRecordsResponseSpec("N1234EX", params).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(response.userMessage).isEqualTo("Validation failure: $message")
  }

  @Test
  fun `200 ok - when no csip record exists for filter conditions`() {
    val response = getCsipRecords("N1234EX")
    assertThat(response.content).isEmpty()
  }

  @Test
  fun `200 ok - csip records details are correctly returned`() {
    val csip = dataSetup(generateCsipRecord().withReferral().withPlan()) { it }

    with(getCsipRecords(csip.prisonNumber)) {
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
  fun `200 ok - csip records matching log code are returned`() {
    val prisoner = prisoner().toPersonLocation()
    dataSetup(generateCsipRecord(prisoner, logCode = "LEI").withReferral()) { it }
    dataSetup(generateCsipRecord(prisoner, logCode = "MDI").withReferral()) { it }

    with(getCsipRecords(prisoner.prisonNumber, mapOf("logCode" to "LEI"))) {
      assertThat(content.size).isEqualTo(1)
      assertThat(content.first().prisonNumber).isEqualTo(prisoner.prisonNumber)
      assertThat(content.first().logCode).isEqualTo("LEI")
    }
  }

  @Test
  fun `200 ok - csip records matching created date are returned`() {
    val prisoner = prisoner().toPersonLocation()
    dataSetup(
      generateCsipRecord(
        prisoner,
        createdAt = LocalDateTime.now().minusDays(1),
        logCode = "NOT_EXP",
      ).withReferral(),
    ) { it }
    dataSetup(
      generateCsipRecord(
        prisoner,
        createdAt = LocalDateTime.now().minusDays(3),
        logCode = "EXP",
      ).withReferral(),
    ) { it }
    dataSetup(
      generateCsipRecord(
        prisoner,
        createdAt = LocalDateTime.now().minusDays(7),
        logCode = "NOT_EXP",
      ).withReferral(),
    ) { it }

    with(
      getCsipRecords(
        prisoner.prisonNumber,
        mapOf(
          "createdAtStart" to LocalDateTime.now().minusDays(5).toString(),
          "createdAtEnd" to LocalDateTime.now().minusDays(2).toString(),
        ),
      ),
    ) {
      assertThat(content.size).isEqualTo(1)
      assertThat(content.first().prisonNumber).isEqualTo(prisoner.prisonNumber)
      assertThat(content.first().logCode).isEqualTo("EXP")
    }
  }

  @Test
  fun `200 ok - default sort is desc`() {
    val prisoner = prisoner().toPersonLocation()
    dataSetup(
      generateCsipRecord(
        prisoner,
        createdAt = LocalDateTime.now().minusDays(3),
        logCode = "TWO",
      ).withReferral(),
    ) { it }
    dataSetup(
      generateCsipRecord(
        prisoner,
        createdAt = LocalDateTime.now().minusDays(1),
        logCode = "ONE",
      ).withReferral(),
    ) { it }
    dataSetup(
      generateCsipRecord(
        prisoner,
        createdAt = LocalDateTime.now().minusDays(7),
        logCode = "THREE",
      ).withReferral(),
    ) { it }

    with(
      getCsipRecords(
        prisoner.prisonNumber,
        mapOf("sort" to "createdAt"),
      ),
    ) {
      assertThat(content.size).isEqualTo(3)
      assertThat(content.map { it.logCode }).containsExactly("ONE", "TWO", "THREE")
    }
  }

  @Test
  fun `200 ok - can sort asc`() {
    val prisoner = prisoner().toPersonLocation()
    dataSetup(
      generateCsipRecord(
        prisoner,
        createdAt = LocalDateTime.now().minusDays(3),
        logCode = "TWO",
      ).withReferral(),
    ) { it }
    dataSetup(
      generateCsipRecord(
        prisoner,
        createdAt = LocalDateTime.now().minusDays(7),
        logCode = "ONE",
      ).withReferral(),
    ) { it }
    dataSetup(
      generateCsipRecord(
        prisoner,
        createdAt = LocalDateTime.now().minusDays(1),
        logCode = "THREE",
      ).withReferral(),
    ) { it }

    with(
      getCsipRecords(
        prisoner.prisonNumber,
        mapOf("sort" to "createdAt,asc"),
      ),
    ) {
      assertThat(content.size).isEqualTo(3)
      assertThat(content.map { it.logCode }).containsExactly("ONE", "TWO", "THREE")
    }
  }

  private fun urlToTest(prisonNumber: String) = "/prisoners/$prisonNumber/csip-records"

  private fun getCsipRecordsResponseSpec(
    prisonNumber: String,
    params: Map<String, String>,
  ): WebTestClient.ResponseSpec =
    webTestClient.get()
      .uri { ub ->
        ub.path(urlToTest(prisonNumber))
        params.forEach {
          ub.queryParam(it.key, it.value)
        }
        ub.build()
      }
      .headers(setAuthorisation(roles = listOfNotNull(ROLE_CSIP_UI)))
      .exchange()

  private fun getCsipRecords(prisonNumber: String, params: Map<String, String> = mapOf()): CsipSummaries =
    getCsipRecordsResponseSpec(prisonNumber, params).successResponse()

  companion object {
    @JvmStatic
    fun parameterValidations() = listOf(
      Arguments.of(mapOf("logCode" to "A".repeat(11)), "Log code must be <= 10 characters"),
      Arguments.of(mapOf("page" to 0.toString()), "Page number must be at least 1"),
      Arguments.of(mapOf("size" to 0.toString()), "Page size must be at least 1"),
      Arguments.of(mapOf("size" to 101.toString()), "Page size must not be more than 100"),
      Arguments.of(mapOf("sort" to "lastModifiedAt"), "sort is invalid"),
    )
  }
}
