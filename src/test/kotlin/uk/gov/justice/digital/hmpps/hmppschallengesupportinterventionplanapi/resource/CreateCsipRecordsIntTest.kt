package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.CONTRIBUTORY_FACTOR
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REFERRAL
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.NO
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LOG_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.verifyAgainst
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class CreateCsipRecordsIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest("A1234BC")).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no required role`() {
    val response = webTestClient.post().uri(urlToTest("A1234BC"))
      .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .bodyValue(createCsipRecordRequest())
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
  fun `403 forbidden - no roles`() {
    webTestClient.post().uri(urlToTest("A1234BC")).bodyValue(createCsipRecordRequest())
      .headers(setAuthorisation(roles = listOf())).exchange().errorResponse(HttpStatus.FORBIDDEN)
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = webTestClient.post().uri(urlToTest("A1234BC")).bodyValue(createCsipRecordRequest())
      .headers(setAuthorisation(user = USER_NOT_FOUND, roles = listOf(ROLE_CSIP_UI)))
      .exchange().errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post().uri(urlToTest("A1234BC"))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).exchange()
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource.CsipRecordsController.createCsipRecord(java.lang.String,uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - prisoner not found`() {
    val request = createCsipRecordRequest()

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER_NOT_FOUND)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prisoner number invalid")
      assertThat(developerMessage).isEqualTo("Prisoner number invalid")
      assertThat(moreInfo).isNull()
    }
  }

  @ParameterizedTest
  @MethodSource("referenceDataValidation")
  fun `400 bad request - when reference data code invalid or inactive`(
    request: CreateCsipRecordRequest,
    invalid: InvalidRd,
  ) {
    val prisonNumber = givenValidPrisonNumber("R1234VC")

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = prisonNumber)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: ${invalid.type} ${invalid.message}")
      assertThat(developerMessage).isEqualTo("Details => ${invalid.type}:${invalid.code(request.referral)}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - multiple contributory factor not found`() {
    val request = createCsipRecordRequest(createReferralRequest(contributoryFactorTypeCode = listOf("D", "E", "F")))

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Multiple invalid CONTRIBUTORY_FACTOR_TYPE")
      assertThat(developerMessage).isEqualTo("Details => CONTRIBUTORY_FACTOR_TYPE:[D,E,F]")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - single contributory factor not found`() {
    val request = createCsipRecordRequest(createReferralRequest(contributoryFactorTypeCode = listOf("D")))

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: CONTRIBUTORY_FACTOR_TYPE is invalid")
      assertThat(developerMessage).isEqualTo("Details => CONTRIBUTORY_FACTOR_TYPE:D")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - single contributory factor not active`() {
    val request = createCsipRecordRequest(
      createReferralRequest(
        incidentTypeCode = "ATO",
        incidentLocationCode = "EDU",
        refererAreaCode = "ACT",
        incidentInvolvementCode = "OTH",
        contributoryFactorTypeCode = listOf("CFT_INACT"),
      ),
    )

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: CONTRIBUTORY_FACTOR_TYPE is not active")
      assertThat(developerMessage).isEqualTo("Details => CONTRIBUTORY_FACTOR_TYPE:CFT_INACT")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - no contributory factors with source DPS`() {
    val request = createCsipRecordRequest(createReferralRequest(contributoryFactorTypeCode = listOf()))

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure: A referral must have at least one contributory factor.")
    }
  }

  @Test
  fun `201 created - CSIP record created via DPS`() {
    val request = createCsipRecordRequest(
      createReferralRequest(contributoryFactorTypeCode = listOf("AFL")),
    )

    val prisonNumber = givenValidPrisonNumber("C1234SP")
    val response = webTestClient.createCsipRecord(prisonNumber, request)

    with(response) {
      assertThat(logCode).isEqualTo(LOG_CODE)
      assertThat(recordUuid).isNotNull()
      assertThat(referral).isNotNull()
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("TEST_USER")
      assertThat(createdByDisplayName).isEqualTo("Test User")
      assertThat(prisonCodeWhenRecorded).isEqualTo(PRISON_CODE_LEEDS)
      assertThat(status).isEqualTo(CsipStatus.REFERRAL_PENDING)
    }

    val saved = csipRecordRepository.getCsipRecord(response.recordUuid)
    saved.verifyAgainst(request)

    verifyAudit(saved, RevisionType.ADD, setOf(RECORD, REFERRAL, CONTRIBUTORY_FACTOR))

    verifyDomainEvents(
      prisonNumber,
      response.recordUuid,
      setOf(RECORD, REFERRAL, CONTRIBUTORY_FACTOR),
      setOf(DomainEventType.CSIP_CREATED, DomainEventType.CONTRIBUTORY_FACTOR_CREATED),
      response.referral.contributoryFactors.map { it.factorUuid }.toSet(),
      2,
    )
  }

  @Test
  fun `201 created - CSIP record created via DPS with empty logCode`() {
    val request = createCsipRecordRequest(
      createReferralRequest(
        contributoryFactorTypeCode = listOf("AFL"),
      ),
      logCode = null,
    )

    val prisonNumber = givenValidPrisonNumber("C1235SP")
    val response = webTestClient.createCsipRecord(prisonNumber, request)

    with(response) {
      assertThat(referral).isNotNull()
      assertThat(logCode).isNull()
    }

    val saved = csipRecordRepository.getCsipRecord(response.recordUuid)
    assertThat(saved.logCode).isNull()
    verifyAudit(saved, RevisionType.ADD, setOf(RECORD, REFERRAL, CONTRIBUTORY_FACTOR))
  }

  @Test
  fun `201 created - completed details returned`() {
    val request = createCsipRecordRequest(createReferralRequest(referralComplete = true))

    val prisonNumber = givenValidPrisonNumber("C1235SP")
    val response = webTestClient.createCsipRecord(prisonNumber, request)

    with(response.referral) {
      assertThat(isReferralComplete).isEqualTo(true)
      assertThat(referralCompletedDate).isEqualTo(LocalDate.now())
      assertThat(referralCompletedBy).isEqualTo(TEST_USER)
      assertThat(referralCompletedByDisplayName).isEqualTo(TEST_USER_NAME)
    }

    val saved = csipRecordRepository.getCsipRecord(response.recordUuid)
    verifyAudit(saved, RevisionType.ADD, setOf(RECORD, REFERRAL, CONTRIBUTORY_FACTOR))
  }

  private fun urlToTest(prisonNumber: String) = "/prisoners/$prisonNumber/csip-records"

  private fun WebTestClient.createCsipResponseSpec(
    user: String = TEST_USER,
    roles: List<String> = listOf(ROLE_CSIP_UI),
    request: CreateCsipRecordRequest,
    prisonNumber: String = PRISON_NUMBER,
  ) = post().uri(urlToTest(prisonNumber)).bodyValue(request).headers(setAuthorisation(user = user, roles = roles))
    .exchange().expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.createCsipRecord(
    prisonNumber: String,
    request: CreateCsipRecordRequest,
    user: String = TEST_USER,
  ) = createCsipResponseSpec(
    user,
    request = request,
    prisonNumber = prisonNumber,
  ).successResponse<CsipRecord>(HttpStatus.CREATED)

  companion object {
    private const val INVALID = "is invalid"
    private const val NOT_ACTIVE = "is not active"

    private fun createCsipRequestWithReferralRd(
      incidentTypeCode: String = "ATO",
      incidentLocationCode: String = "EDU",
      refererAreaCode: String = "ACT",
      incidentInvolvementCode: String = "OTH",
    ) = createCsipRecordRequest(
      createReferralRequest(
        incidentTypeCode,
        incidentLocationCode,
        refererAreaCode,
        incidentInvolvementCode,
      ),
    )

    @JvmStatic
    fun referenceDataValidation() = listOf(
      Arguments.of(
        createCsipRequestWithReferralRd(incidentTypeCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_TYPE, CreateReferralRequest::incidentTypeCode, INVALID),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(incidentLocationCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_LOCATION, CreateReferralRequest::incidentLocationCode, INVALID),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(refererAreaCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.AREA_OF_WORK, CreateReferralRequest::refererAreaCode, INVALID),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(incidentInvolvementCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_INVOLVEMENT, { it.incidentInvolvementCode!! }, INVALID),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(incidentTypeCode = "IT_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_TYPE, CreateReferralRequest::incidentTypeCode, NOT_ACTIVE),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(incidentLocationCode = "IL_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_LOCATION, CreateReferralRequest::incidentLocationCode, NOT_ACTIVE),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(refererAreaCode = "AOW_INACT"),
        InvalidRd(ReferenceDataType.AREA_OF_WORK, CreateReferralRequest::refererAreaCode, NOT_ACTIVE),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(incidentInvolvementCode = "II_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_INVOLVEMENT, { it.incidentInvolvementCode!! }, NOT_ACTIVE),
      ),
    )

    data class InvalidRd(
      val type: ReferenceDataType,
      val code: (CreateReferralRequest) -> String,
      val message: String,
    )

    private fun createCsipRecordRequest(
      createReferralRequest: CreateReferralRequest = createReferralRequest(),
      logCode: String? = LOG_CODE,
    ) = CreateCsipRecordRequest(
      logCode,
      createReferralRequest,
    )

    private fun createReferralRequest(
      incidentTypeCode: String = "ATO",
      incidentLocationCode: String = "EDU",
      refererAreaCode: String = "ACT",
      incidentInvolvementCode: String = "OTH",
      contributoryFactorTypeCode: Collection<String> = listOf("AFL"),
      referralComplete: Boolean? = null,
    ) = CreateReferralRequest(
      LocalDate.now(),
      LocalTime.now(),
      incidentTypeCode,
      incidentLocationCode,
      "REFERRER",
      refererAreaCode,
      isProactiveReferral = false,
      isStaffAssaulted = false,
      "",
      incidentInvolvementCode,
      "concern description",
      "known reasons",
      "",
      NO,
      referralComplete,
      contributoryFactorTypeCode.map { createContributoryFactorRequest(it) },
    )
  }
}
