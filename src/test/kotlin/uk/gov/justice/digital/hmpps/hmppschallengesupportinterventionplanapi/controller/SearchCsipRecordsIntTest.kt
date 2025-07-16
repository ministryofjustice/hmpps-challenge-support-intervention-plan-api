package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.jpa.domain.Specification
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus.NO_FURTHER_ACTION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResult
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResults
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.FindCsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import java.net.URLDecoder.decode
import java.time.LocalDate
import kotlin.text.Charsets.UTF_8

class SearchCsipRecordsIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(BASE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no required role`() {
    val response = searchCsipRecordsResponseSpec(searchReq(), "WRONG_ROLE").errorResponse(HttpStatus.FORBIDDEN)

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
  fun `400 bad request - at least one prison code or a person identifier must be provided`() {
    val request = searchRequest(prisonCode = emptySet())
    val response = searchCsipRecordsResponseSpec(request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(response.userMessage).isEqualTo("Validation failure: At least one prison code or a prison number must be provided")
  }

  @Test
  fun `200 ok - can find all csip records for a person regardless of prison`() {
    val personSummary = prisoner(prisonId = "OUT", status = "INACTIVE OUT", cellLocation = null).toPersonSummary()
    val csip1 = dataSetup(generateCsipRecord(personSummary)) {
      it.withCompletedReferral(referralDate = LocalDate.now().minusDays(7)).withPlan()
      requireNotNull(it.plan).withReview(actions = sortedSetOf(ReviewAction.CLOSE_CSIP))
      it
    }
    val csip2 = dataSetup(generateCsipRecord(personSummary)) {
      it.withCompletedReferral(referralDate = LocalDate.now().minusDays(2))
    }

    val csips = csipRecordRepository.findAll()

    val response = searchCsipRecords(searchReq(prisonCode = null, query = personSummary.prisonNumber))
    assertThat(response.content.map { it.id }).containsExactly(csip2.id, csip1.id)
    response.content.first().verifyAgainst(csip2)
    response.content.last().verifyAgainst(csip1)
  }

  @Test
  fun `200 ok - when no csip record exists for filter conditions`() {
    val response = searchCsipRecords(searchReq("UNKNOWN"))
    assertThat(response.content).isEmpty()
  }

  @Test
  fun `200 ok - csip records details are correctly returned`() {
    val prisonCode1 = "SPE"
    val prisonCode2 = "OTH"
    clearDown(prisonCode1, prisonCode2)

    val csip1 = dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode1).toPersonSummary())) {
      it.withReferral().withPlan().plan!!.withReview()
      it
    }
    val csip2 = dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode2).toPersonSummary())) { it.withReferral() }

    val res1 = searchCsipRecords(searchReq(prisonCode = prisonCode1))
    assertThat(res1.content.size).isEqualTo(1)
    res1.content.first().verifyAgainst(csip1)

    val res2 = searchCsipRecords(searchReq(prisonCode = prisonCode2))
    assertThat(res2.content.size).isEqualTo(1)
    res2.content.first().verifyAgainst(csip2)
  }

  @Test
  fun `200 ok - csip records details are returned based on prison codes`() {
    val prisonCodes = arrayOf("MAC", "CAN")
    clearDown(*prisonCodes)

    dataSetup(generateCsipRecord(prisoner(prisonId = prisonCodes[0]).toPersonSummary())) {
      it.withReferral().withPlan().plan!!.withReview()
      it
    }
    dataSetup(generateCsipRecord(prisoner(supportingPrisonId = prisonCodes[0]).toPersonSummary())) {
      it.withReferral().withPlan().plan!!.withReview()
      it
    }
    dataSetup(generateCsipRecord(prisoner(prisonId = prisonCodes[1]).toPersonSummary())) {
      it.withReferral().withPlan().plan!!.withReview()
      it
    }
    dataSetup(generateCsipRecord(prisoner(supportingPrisonId = prisonCodes[1]).toPersonSummary())) {
      it.withReferral().withPlan().plan!!.withReview()
      it
    }

    dataSetup(generateCsipRecord(prisoner(prisonId = "OTH").toPersonSummary())) { it.withReferral() }

    val res1 = searchCsipRecords(searchRequest(prisonCode = prisonCodes.toSet()))
    assertThat(res1.content.size).isEqualTo(4)
  }

  @Test
  fun `200 ok - can find for a prison number`() {
    clearDown(SEARCH_PRISON_CODE)
    val csip1 = dataSetup(generateCsipRecord(prisoner(prisonId = SEARCH_PRISON_CODE).toPersonSummary())) {
      it.withReferral().withPlan()
    }
    dataSetup(generateCsipRecord(prisoner(prisonId = SEARCH_PRISON_CODE).toPersonSummary())) {
      it.withReferral().withPlan()
    }

    val res = searchCsipRecords(searchReq(prisonCode = SEARCH_PRISON_CODE, query = csip1.prisonNumber))
    assertThat(res.content.size).isEqualTo(1)
    res.content.first().verifyAgainst(csip1)

    val res2 = searchCsipRecords(
      searchReq(
        prisonCode = SEARCH_PRISON_CODE,
        query = decode("${csip1.prisonNumber}%00", UTF_8.name()),
      ),
    )
    assertThat(res2.content.size).isEqualTo(1)
    res2.content.first().verifyAgainst(csip1)
  }

  @Test
  fun `200 ok - can include or exclude restricted patients`() {
    val prisonCode = "RESP"
    clearDown(prisonCode)

    val csip1 = dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode).toPersonSummary())) {
      it.withReferral().withPlan()
    }
    dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode, restrictedPatient = true).toPersonSummary())) {
      it.withReferral().withPlan()
    }

    val res1 = searchCsipRecords(searchReq(prisonCode = prisonCode))
    assertThat(res1.content.size).isEqualTo(1)
    res1.content.first().verifyAgainst(csip1)

    val res2 = searchCsipRecords(searchReq(prisonCode = prisonCode, includeRestrictedPatients = true))
    assertThat(res2.content.size).isEqualTo(2)
  }

  @Test
  fun `200 ok - can find and sort by name`() {
    clearDown(SEARCH_PRISON_CODE)

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

    val csip5 = dataSetup(
      generateCsipRecord(
        prisoner(prisonId = SEARCH_PRISON_CODE, firstName = "Darren", lastName = "Jumbo").toPersonSummary(),
      ),
    ) {
      it.withReferral().withPlan()
    }

    verifySort(SEARCH_PRISON_CODE, "name", listOf(csip3, csip1, csip2), query = "john")
    verifySort(SEARCH_PRISON_CODE, "name", listOf(csip3, csip1, csip4), query = "jam")
    verifySort(SEARCH_PRISON_CODE, "name", listOf(csip4), query = "smith james")
    verifySort(SEARCH_PRISON_CODE, "name", listOf(csip3, csip1, csip5, csip4), query = "j_m")
    verifySort(SEARCH_PRISON_CODE, "name", listOf(csip5), query = "r%e")
    verifySort(SEARCH_PRISON_CODE, "name", listOf(csip5), query = decode("ren%00", UTF_8.name()))
  }

  @Test
  fun `200 ok - can find by status`() {
    val prisonCode = "STA"
    clearDown(prisonCode)

    val closedCsip = dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode).toPersonSummary())) {
      it.withReferral().withPlan()
      requireNotNull(it.plan).withReview(actions = sortedSetOf(ReviewAction.CLOSE_CSIP))
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
    val noFurtherAction = dataSetup(generateCsipRecord(prisoner(prisonId = prisonCode).toPersonSummary())) {
      it.withReferral()
      val referral = requireNotNull(it.referral).withInvestigation()
      requireNotNull(referral.investigation).withInterview()
      referral.withDecisionAndActions(givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "NFA"))
      it
    }

    val res1 = searchCsipRecords(searchReq(prisonCode = prisonCode, status = CSIP_CLOSED, size = 100))
    assertThat(res1.content.size).isEqualTo(1)
    res1.content.first().verifyAgainst(closedCsip)

    val res2 = searchCsipRecords(searchReq(prisonCode = prisonCode, status = CSIP_OPEN, size = 100))
    assertThat(res2.content.size).isEqualTo(1)
    res2.content.first().verifyAgainst(openCsip)

    val res3 = searchCsipRecords(searchReq(prisonCode = prisonCode, status = AWAITING_DECISION, size = 100))
    assertThat(res3.content.size).isEqualTo(1)
    res3.content.first().verifyAgainst(awaitingDecision)

    val res4 = searchCsipRecords(searchReq(prisonCode = prisonCode, status = NO_FURTHER_ACTION, size = 100))
    assertThat(res4.content.size).isEqualTo(1)
    res4.content.first().verifyAgainst(noFurtherAction)
  }

  @Test
  fun `200 ok - can sort by fields`() {
    val prisonCode = "SOR"
    clearDown(prisonCode)

    val csip1 = dataSetup(
      generateCsipRecord(
        // logCode = "S0442",
        personSummary = prisoner(
          prisonId = prisonCode,
          cellLocation = "$prisonCode-1-123",
          lastName = "Johnson",
          firstName = "James",
        ).toPersonSummary(),
      ),
    ) {
      it.withCompletedReferral(
        referralDate = LocalDate.now().minusDays(10),
        incidentType = { givenReferenceData(ReferenceDataType.INCIDENT_TYPE, "FTE") },
      ).withPlan(caseManager = "John Doe").plan!!.withReview(nextReviewDate = LocalDate.now().plusWeeks(12))
      it
    }
    val csip2 = dataSetup(
      generateCsipRecord(
        logCode = "S0242",
        personSummary = prisoner(
          prisonId = prisonCode,
          cellLocation = "$prisonCode-2-123",
          lastName = "Smith",
          firstName = "Jane",
        ).toPersonSummary(),
      ),
    ) {
      it.withCompletedReferral(
        referralDate = LocalDate.now().minusDays(30),
        incidentType = { givenReferenceData(ReferenceDataType.INCIDENT_TYPE, "MDO") },
      )
      requireNotNull(it.referral).withInvestigation()
      it
    }

    val csip3 = dataSetup(
      generateCsipRecord(
        logCode = "S0342",
        personSummary = prisoner(
          prisonId = prisonCode,
          cellLocation = "$prisonCode-2-456",
          lastName = "Smith",
          firstName = "John",
        ).toPersonSummary(),
      ),
    ) {
      it.withReferral(
        referralDate = LocalDate.now().minusDays(90),
        incidentType = { givenReferenceData(ReferenceDataType.INCIDENT_TYPE, "OTH") },
      )
    }

    val csip4 = dataSetup(
      generateCsipRecord(
        logCode = "S0142",
        personSummary = prisoner(
          prisonId = prisonCode,
          cellLocation = "$prisonCode-3-123",
          lastName = "James",
          firstName = "Laura",
        ).toPersonSummary(),
      ),
    ) {
      it.withCompletedReferral(
        referralDate = LocalDate.now().minusDays(10),
        incidentType = { givenReferenceData(ReferenceDataType.INCIDENT_TYPE, "OTH") },
      ).withPlan(caseManager = "Jane Doe", firstCaseReviewDate = LocalDate.now().plusWeeks(6))
      it
    }

    verifySort(prisonCode, "location", listOf(csip1, csip2, csip3, csip4))
    verifySort(prisonCode, "referralDate", listOf(csip3, csip2, csip1, csip4), listOf(csip4, csip1, csip2, csip3))
    verifySort(prisonCode, "nextReviewDate", listOf(csip4, csip1, csip2, csip3), listOf(csip2, csip3, csip1, csip4))
    verifySort(prisonCode, "status", listOf(csip2, csip4, csip1, csip3), listOf(csip3, csip4, csip1, csip2))
    verifySort(prisonCode, "caseManager", listOf(csip4, csip1, csip2, csip3), listOf(csip2, csip3, csip1, csip4))
    verifySort(prisonCode, "logCode", listOf(csip4, csip2, csip3, csip1))
    verifySort(prisonCode, "incidentType", listOf(csip1, csip2, csip4, csip3), listOf(csip4, csip3, csip2, csip1))
  }

  private fun verifySort(
    prisonCode: String,
    sortField: String,
    order: List<CsipRecord>,
    reverseOrder: List<CsipRecord> = order.reversed(),
    query: String? = null,
  ) {
    val res1 = searchCsipRecords(searchReq(prisonCode = prisonCode, query = query, sort = "$sortField,asc"))
    assertThat(res1.content.size).isEqualTo(order.size)
    for (i in order.indices) {
      res1.content[i].verifyAgainst(order[i])
    }

    val res2 = searchCsipRecords(searchReq(prisonCode = prisonCode, query = query, sort = "$sortField,desc"))
    assertThat(res2.content.size).isEqualTo(order.size)
    for (i in reverseOrder.indices) {
      res2.content[i].verifyAgainst(reverseOrder[i])
    }
  }

  private fun searchCsipRecordsResponseSpec(
    request: FindCsipRequest,
    role: String = ROLE_CSIP_UI,
  ): WebTestClient.ResponseSpec = webTestClient.get()
    .uri { ub ->
      ub.path(BASE_URL)
      request.asParams().forEach {
        ub.queryParam(it.first, it.second)
      }
      ub.build()
    }
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .exchange()

  private fun searchCsipRecords(request: FindCsipRequest): CsipSearchResults = searchCsipRecordsResponseSpec(request).successResponse()

  companion object {
    private const val BASE_URL = "/search/csip-records"
    private const val SEARCH_PRISON_CODE = "SWI"

    private fun searchReq(
      prisonCode: String? = SEARCH_PRISON_CODE,
      query: String? = null,
      status: CsipStatus? = null,
      includeRestrictedPatients: Boolean? = null,
      page: Int = 1,
      size: Int = 10,
      sort: String = "referralDate,desc",
    ) = searchRequest(
      prisonCode?.let { setOf(it) } ?: emptySet(),
      query,
      status?.let { setOf(it) } ?: emptySet(),
      includeRestrictedPatients,
      page,
      size,
      sort,
    )

    private fun searchRequest(
      prisonCode: Set<String> = setOf(SEARCH_PRISON_CODE),
      query: String? = null,
      status: Set<CsipStatus> = setOf(),
      includeRestrictedPatients: Boolean? = null,
      page: Int = 1,
      size: Int = 10,
      sort: String = "referralDate,desc",
    ) = FindCsipRequest(prisonCode, query, status, includeRestrictedPatients == true, page, size, sort)

    @JvmStatic
    fun parameterValidations() = listOf(
      Arguments.of(searchReq(page = 0), "Page number must be at least 1"),
      Arguments.of(searchReq(size = 0), "Page size must be at least 1"),
      Arguments.of(searchReq(size = 101), "Page size must not be more than 100"),
      Arguments.of(searchReq(sort = "invalidField"), "sort is invalid"),
    )
  }

  private fun FindCsipRequest.asParams() = listOfNotNull(
    listOfNotNull(
      query?.let { "query" to it },
      includeRestrictedPatients.takeIf { it }?.let { "includeRestrictedPatients" to true },
      "page" to page,
      "size" to size,
      "sort" to sort,
    ),
    prisonCode.map { "prisonCode" to it },
    status.map { "status" to it },
  ).flatten()

  private fun CsipSearchResult.verifyAgainst(csip: CsipRecord) {
    assertThat(id).isEqualTo(csip.id)
    assertThat(logCode).isEqualTo(csip.logCode)
    assertThat(status.code).isEqualTo(csip.status!!.code)
    assertThat(referralDate).isEqualTo(csip.referral!!.referralDate)
    assertThat(nextReviewDate).isEqualTo(csip.plan?.nextReviewDate)
    assertThat(caseManager).isEqualTo(csip.plan?.caseManager)
    assertThat(incidentType).isEqualTo(csip.referral?.incidentType?.description)
    prisoner.verifyAgainst(csip.personSummary)
  }

  private fun Prisoner.verifyAgainst(person: PersonSummary) {
    assertThat(prisonNumber).isEqualTo(person.prisonNumber)
    assertThat(firstName).isEqualTo(person.firstName)
    assertThat(lastName).isEqualTo(person.lastName)
    assertThat(location).isEqualTo(person.cellLocation)
  }

  private fun matchesPrison(vararg prisonCodes: String) = Specification<CsipRecord> { csip, _, cb ->
    val person = csip.join<CsipRecord, PersonSummary>("personSummary")
    cb.or(
      person.get<String>("prisonCode").`in`(prisonCodes.toSet()),
      person.get<String>("supportingPrisonCode").`in`(prisonCodes.toSet()),
    )
  }

  private fun clearDown(vararg prisonCodes: String) = transactionTemplate.executeWithoutResult {
    csipRecordRepository.findAll(matchesPrison(*prisonCodes)).forEach {
      csipRecordRepository.delete(it)
      personSummaryRepository.delete(it.personSummary)
    }
  }
}
