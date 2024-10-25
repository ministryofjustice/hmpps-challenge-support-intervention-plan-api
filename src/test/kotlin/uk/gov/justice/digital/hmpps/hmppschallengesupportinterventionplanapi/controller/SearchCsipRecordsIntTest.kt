package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus.AWAITING_DECISION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus.CSIP_CLOSED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus.CSIP_OPEN
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResult
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResults
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.FindCsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import java.time.LocalDate

class SearchCsipRecordsIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(BASE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no required role`() {
    val response = searchCsipRecordsResponseSpec(searchRequest(), "WRONG_ROLE").errorResponse(HttpStatus.FORBIDDEN)

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
    request: FindCsipRequest,
    message: String,
  ) {
    val response = searchCsipRecordsResponseSpec(request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(response.userMessage).isEqualTo("Validation failure: $message")
  }

  @Test
  fun `200 ok - when no csip record exists for filter conditions`() {
    val response = searchCsipRecords(searchRequest("UNKNOWN"))
    assertThat(response.content).isEmpty()
  }

  @Test
  fun `200 ok - csip records details are correctly returned`() {
    val prisonCode1 = "SPE"
    val csip1 = dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode1).toPersonSummary())) {
      it.withReferral().withPlan().plan!!.withReview()
      it
    }
    val prisonCode2 = "OTH"
    val csip2 = dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode2).toPersonSummary())) { it.withReferral() }

    val res1 = searchCsipRecords(searchRequest(prisonCode = prisonCode1))
    assertThat(res1.content.size).isEqualTo(1)
    res1.content.first().verifyAgainst(csip1)

    val res2 = searchCsipRecords(searchRequest(prisonCode = prisonCode2))
    assertThat(res2.content.size).isEqualTo(1)
    res2.content.first().verifyAgainst(csip2)
  }

  @Test
  fun `200 ok - can find for a prison number`() {
    val csip1 = dataSetup(generateCsipRecord(prisoner(prisonId = SEARCH_PRISON_CODE).toPersonSummary())) {
      it.withReferral().withPlan()
    }
    dataSetup(generateCsipRecord(prisoner(prisonId = SEARCH_PRISON_CODE).toPersonSummary())) {
      it.withReferral().withPlan()
    }

    val res = searchCsipRecords(searchRequest(prisonCode = SEARCH_PRISON_CODE, query = csip1.prisonNumber))
    assertThat(res.content.size).isEqualTo(1)
    res.content.first().verifyAgainst(csip1)
  }

  @Test
  fun `200 ok - can find and sort by name`() {
    val csip1 = dataSetup(
      generateCsipRecord(
        prisoner(prisonId = SEARCH_PRISON_CODE, firstName = "James", lastName = "Johnson").toPersonSummary(),
      ),
    ) {
      it.withReferral().withPlan()
    }
    val csip2 = dataSetup(
      generateCsipRecord(
        prisoner(prisonId = SEARCH_PRISON_CODE, firstName = "Jane", lastName = "Johnson").toPersonSummary(),
      ),
    ) {
      it.withReferral().withPlan().plan!!.withReview()
      it
    }
    val csip3 = dataSetup(
      generateCsipRecord(
        prisoner(prisonId = SEARCH_PRISON_CODE, firstName = "John", lastName = "James").toPersonSummary(),
      ),
    ) {
      it.withReferral().withPlan()
    }

    val csip4 = dataSetup(
      generateCsipRecord(
        prisoner(prisonId = SEARCH_PRISON_CODE, firstName = "James", lastName = "Smith").toPersonSummary(),
      ),
    ) {
      it.withReferral().withPlan()
    }

    verifySort(SEARCH_PRISON_CODE, "name", listOf(csip3, csip1, csip2), query = "john")
    verifySort(SEARCH_PRISON_CODE, "name", listOf(csip3, csip1, csip4), query = "jam")
  }

  @Test
  fun `200 ok - can find by status`() {
    val prisonCode = "STA"
    val closedCsip = dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode).toPersonSummary())) {
      it.withReferral().withPlan()
      requireNotNull(it.plan).withReview(actions = setOf(ReviewAction.CLOSE_CSIP))
      it
    }
    val openCsip = dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode).toPersonSummary())) {
      it.withReferral().withPlan()
    }
    val awaitingDecision = dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode).toPersonSummary())) {
      it.withReferral()
      val referral = requireNotNull(it.referral).withInvestigation()
      requireNotNull(referral.investigation).withInterview()
      it
    }

    val res1 = searchCsipRecords(searchRequest(prisonCode = prisonCode, status = CSIP_CLOSED, size = 100))
    assertThat(res1.content.size).isEqualTo(1)
    res1.content.first().verifyAgainst(closedCsip)

    val res2 = searchCsipRecords(searchRequest(prisonCode = prisonCode, status = CSIP_OPEN, size = 100))
    assertThat(res2.content.size).isEqualTo(1)
    res2.content.first().verifyAgainst(openCsip)

    val res3 = searchCsipRecords(searchRequest(prisonCode = prisonCode, status = AWAITING_DECISION, size = 100))
    assertThat(res3.content.size).isEqualTo(1)
    res3.content.first().verifyAgainst(awaitingDecision)
  }

  @Test
  fun `200 ok - can sort by fields`() {
    val prisonCode = "SOR"
    val csip1 = dataSetup(
      generateCsipRecord(
        prisoner(
          prisonId = prisonCode,
          cellLocation = "$prisonCode-1-123",
          lastName = "Johnson",
          firstName = "James",
        ).toPersonSummary(),
      ),
    ) {
      it.withCompletedReferral(referralDate = LocalDate.now().minusDays(10))
        .withPlan(caseManager = "John Doe").plan!!.withReview(nextReviewDate = LocalDate.now().plusWeeks(12))
      it
    }
    val csip2 = dataSetup(
      generateCsipRecord(
        prisoner(
          prisonId = prisonCode,
          cellLocation = "$prisonCode-2-123",
          lastName = "Smith",
          firstName = "Jane",
        ).toPersonSummary(),
      ),
    ) {
      it.withCompletedReferral(referralDate = LocalDate.now().minusDays(30))
      requireNotNull(it.referral).withInvestigation()
      it
    }

    val csip3 = dataSetup(
      generateCsipRecord(
        prisoner(
          prisonId = prisonCode,
          cellLocation = "$prisonCode-2-456",
          lastName = "Smith",
          firstName = "John",
        ).toPersonSummary(),
      ),
    ) {
      it.withReferral(referralDate = LocalDate.now().minusDays(90))
    }

    val csip4 = dataSetup(
      generateCsipRecord(
        prisoner(
          prisonId = prisonCode,
          cellLocation = "$prisonCode-3-123",
          lastName = "James",
          firstName = "Laura",
        ).toPersonSummary(),
      ),
    ) {
      it.withCompletedReferral(referralDate = LocalDate.now().minusDays(10))
        .withPlan(caseManager = "Jane Doe", firstCaseReviewDate = LocalDate.now().plusWeeks(6))
      it
    }

    verifySort(prisonCode, "location", listOf(csip1, csip2, csip3, csip4))
    verifySort(prisonCode, "referralDate", listOf(csip3, csip2, csip4, csip1), listOf(csip4, csip1, csip2, csip3))
    verifySort(prisonCode, "nextReviewDate", listOf(csip4, csip1, csip2, csip3), listOf(csip2, csip3, csip1, csip4))
    verifySort(prisonCode, "status", listOf(csip2, csip4, csip1, csip3), listOf(csip3, csip4, csip1, csip2))
    verifySort(prisonCode, "caseManager", listOf(csip4, csip1, csip2, csip3), listOf(csip2, csip3, csip1, csip4))
  }

  private fun verifySort(
    prisonCode: String,
    sortField: String,
    order: List<CsipRecord>,
    reverseOrder: List<CsipRecord> = order.reversed(),
    query: String? = null,
  ) {
    val res1 = searchCsipRecords(searchRequest(prisonCode = prisonCode, query = query, sort = "$sortField,asc"))
    assertThat(res1.content.size).isEqualTo(order.size)
    for (i in order.indices) {
      res1.content[i].verifyAgainst(order[i])
    }

    val res2 = searchCsipRecords(searchRequest(prisonCode = prisonCode, query = query, sort = "$sortField,desc"))
    assertThat(res2.content.size).isEqualTo(order.size)
    for (i in reverseOrder.indices) {
      res2.content[i].verifyAgainst(reverseOrder[i])
    }
  }

  private fun searchCsipRecordsResponseSpec(
    request: FindCsipRequest,
    role: String = ROLE_CSIP_UI,
  ): WebTestClient.ResponseSpec =
    webTestClient.get()
      .uri { ub ->
        ub.path(BASE_URL)
        request.asParams().forEach {
          ub.queryParam(it.key, it.value)
        }
        ub.build()
      }
      .headers(setAuthorisation(roles = listOfNotNull(role)))
      .exchange()

  private fun searchCsipRecords(request: FindCsipRequest): CsipSearchResults =
    searchCsipRecordsResponseSpec(request).successResponse()

  companion object {
    private const val BASE_URL = "/search/csip-records"
    private const val SEARCH_PRISON_CODE = "SWI"

    private fun searchRequest(
      prisonCode: String = SEARCH_PRISON_CODE,
      query: String? = null,
      status: CsipStatus? = null,
      page: Int = 1,
      size: Int = 10,
      sort: String = "referralDate,desc",
    ) = FindCsipRequest(prisonCode, query, status, page, size, sort)

    @JvmStatic
    fun parameterValidations() = listOf(
      Arguments.of(searchRequest(page = 0), "Page number must be at least 1"),
      Arguments.of(searchRequest(size = 0), "Page size must be at least 1"),
      Arguments.of(searchRequest(size = 101), "Page size must not be more than 100"),
      Arguments.of(searchRequest(sort = "logCode"), "sort is invalid"),
    )
  }

  private fun FindCsipRequest.asParams() = listOfNotNull(
    "prisonCode" to prisonCode,
    query?.let { "query" to it },
    status?.let { "status" to it },
    "page" to page,
    "size" to size,
    "sort" to sort,
  ).toMap()

  private fun CsipSearchResult.verifyAgainst(csip: CsipRecord) {
    assertThat(id).isEqualTo(csip.id)
    assertThat(status).isEqualTo(csip.status)
    assertThat(referralDate).isEqualTo(csip.referral!!.referralDate)
    assertThat(nextReviewDate).isEqualTo(csip.plan?.nextReviewDate())
    assertThat(caseManager).isEqualTo(csip.plan?.caseManager)
    prisoner.verifyAgainst(csip.personSummary)
  }

  private fun Prisoner.verifyAgainst(person: PersonSummary) {
    assertThat(prisonNumber).isEqualTo(person.prisonNumber)
    assertThat(firstName).isEqualTo(person.firstName)
    assertThat(lastName).isEqualTo(person.lastName)
    assertThat(location).isEqualTo(person.cellLocation)
  }
}